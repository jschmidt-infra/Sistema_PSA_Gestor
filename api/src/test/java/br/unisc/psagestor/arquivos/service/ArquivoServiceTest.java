package br.unisc.psagestor.arquivos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.unisc.psagestor.comum.api.FalhaApi;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class ArquivoServiceTest {
    private final ArquivoService service =
            new ArquivoService(mock(ArquivoStorage.class), mock(ArquivoPendenteService.class));

    private byte[] png() throws Exception {
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", bytes);
        return bytes.toByteArray();
    }

    @Test
    void identificaConteudoENormalizaNomeSemPerderAcentos() throws Exception {
        byte[] bytes = png();
        var conteudo =
                service.validar(
                        new MockMultipartFile("arquivo", "croqui/área.png", "image/png", bytes));
        assertThat(conteudo.mime()).isEqualTo("image/png");
        assertThat(conteudo.nome()).isEqualTo("croqui_área.png");
        assertThat(conteudo.bytes()).isEqualTo(bytes);
    }

    @Test
    void rejeitaMimeQueNaoCorrespondeAoConteudo() throws Exception {
        var arquivo = new MockMultipartFile("arquivo", "croqui.pdf", "application/pdf", png());
        assertThatThrownBy(() -> service.validar(arquivo))
                .isInstanceOfSatisfying(
                        FalhaApi.class, falha -> assertThat(falha.status()).isEqualTo(415));
    }

    @Test
    void rejeitaArquivoGrandeAntesDeCarregarConteudo() throws Exception {
        var arquivo = mock(MultipartFile.class);
        when(arquivo.getSize()).thenReturn(10L * 1024 * 1024 + 1);
        assertThatThrownBy(() -> service.validar(arquivo))
                .isInstanceOfSatisfying(
                        FalhaApi.class, falha -> assertThat(falha.status()).isEqualTo(413));
        verify(arquivo, never()).getBytes();
    }
}
