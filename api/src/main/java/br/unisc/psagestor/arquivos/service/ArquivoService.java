package br.unisc.psagestor.arquivos.service;

import br.unisc.psagestor.comum.api.FalhaApi;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Valida conteúdo e coordena objetos privados com o diário persistente de limpeza. */
@Service
public class ArquivoService {
    private static final int TAMANHO_MAXIMO_BYTES = 10 * 1024 * 1024;
    private static final int MAXIMO_PAGINAS_PDF = 100;
    private static final long MAXIMO_PIXELS_IMAGEM = 25_000_000L;
    private static final int TAMANHO_MAXIMO_NOME = 200;
    private static final Set<String> FORMATOS_IMAGEM = Set.of("png", "jpeg", "jpg");

    private final ArquivoStorage storage;
    private final ArquivoPendenteService pendentes;

    public ArquivoService(ArquivoStorage storage, ArquivoPendenteService pendentes) {
        this.storage = storage;
        this.pendentes = pendentes;
    }

    public record Conteudo(byte[] bytes, String mime, String nome) {}

    public Conteudo validar(MultipartFile arquivo) {
        if (arquivo.isEmpty() || arquivo.getSize() > TAMANHO_MAXIMO_BYTES) {
            throw new FalhaApi(
                    413, "TAMANHO_INVALIDO", "Envie um arquivo não vazio com até 10 MiB.");
        }
        try {
            byte[] bytes = arquivo.getBytes();
            // Extensão e cabeçalho enviados pelo cliente não comprovam o conteúdo do arquivo.
            String mime = temCabecalhoPdf(bytes) ? validarPdf(bytes) : validarImagem(bytes);
            if (arquivo.getContentType() != null && !arquivo.getContentType().equals(mime)) {
                throw new IOException("O tipo informado difere do conteúdo.");
            }
            return new Conteudo(bytes, mime, normalizarNome(arquivo.getOriginalFilename()));
        } catch (IOException | IllegalArgumentException erro) {
            throw new FalhaApi(
                    415,
                    "TIPO_INVALIDO",
                    "Envie PDF, PNG ou JPEG válido, sem criptografia e dentro dos limites de conteúdo.");
        }
    }

    private boolean temCabecalhoPdf(byte[] bytes) {
        return bytes.length >= 5
                && bytes[0] == '%'
                && bytes[1] == 'P'
                && bytes[2] == 'D'
                && bytes[3] == 'F'
                && bytes[4] == '-';
    }

    private String validarPdf(byte[] bytes) throws IOException {
        try (var pdf = Loader.loadPDF(bytes)) {
            if (pdf.isEncrypted()
                    || pdf.getNumberOfPages() > MAXIMO_PAGINAS_PDF
                    || pdf.getNumberOfPages() == 0) {
                throw new IOException("PDF fora dos limites permitidos.");
            }
        }
        return "application/pdf";
    }

    private String validarImagem(byte[] bytes) throws IOException {
        try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (input == null) {
                throw new IOException("Conteúdo de imagem indisponível.");
            }
            var leitores = ImageIO.getImageReaders(input);
            if (!leitores.hasNext()) {
                throw new IOException("Formato de imagem não reconhecido.");
            }
            var leitor = leitores.next();
            try {
                leitor.setInput(input);
                String formato = leitor.getFormatName().toLowerCase(Locale.ROOT);
                // Confere dimensões antes de decodificar, limitando a memória da imagem expandida.
                if (!FORMATOS_IMAGEM.contains(formato)
                        || (long) leitor.getWidth(0) * leitor.getHeight(0) > MAXIMO_PIXELS_IMAGEM) {
                    throw new IOException("Imagem fora dos limites permitidos.");
                }
                if (leitor.read(0) == null) {
                    throw new IOException("Imagem inválida.");
                }
                return formato.equals("png") ? "image/png" : "image/jpeg";
            } finally {
                leitor.dispose();
            }
        }
    }

    private String normalizarNome(String original) {
        String nome =
                Optional.ofNullable(original)
                        .orElse("arquivo")
                        .replaceAll("[^\\p{L}\\p{N}._ -]", "_");
        return nome.length() > TAMANHO_MAXIMO_NOME
                ? nome.substring(nome.length() - TAMANHO_MAXIMO_NOME)
                : nome;
    }

    public String guardar(byte[] bytes, String mime) {
        String chave = UUID.randomUUID().toString();
        // O diário é confirmado antes do upload: um rollback posterior não deixa um objeto sem
        // rastreio.
        pendentes.registrar(chave);
        storage.guardar(chave, new ByteArrayInputStream(bytes), bytes.length, mime);
        return chave;
    }

    public byte[] abrir(String chave) {
        try (var input = storage.abrir(chave)) {
            byte[] bytes = input.readNBytes(TAMANHO_MAXIMO_BYTES + 1);
            if (bytes.length > TAMANHO_MAXIMO_BYTES) {
                throw new IOException("Objeto acima do limite de leitura.");
            }
            return bytes;
        } catch (IOException erro) {
            throw new FalhaApi(503, "ARQUIVO_INDISPONIVEL", "Não foi possível ler o arquivo.");
        }
    }

    public void compensar(String chave) {
        storage.removerObjetoNovo(chave);
    }

    public void obsoleto(String chave) {
        if (chave != null) {
            pendentes.registrar(chave);
        }
    }
}
