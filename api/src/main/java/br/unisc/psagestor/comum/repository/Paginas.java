package br.unisc.psagestor.comum.repository;

import br.unisc.psagestor.comum.api.Dados;
import br.unisc.psagestor.comum.api.Pagina;
import java.util.Map;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class Paginas {
    private Paginas() {}

    public static Pageable pedido(int page, int size) {
        Pagina.validar(page, size);
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
    }

    public static <T> Pagina<Map<String, Object>> saida(
            Page<T> p, Function<T, Map<String, Object>> converter) {
        return new Pagina<>(
                p.getContent().stream().map(converter).map(Dados::saida).toList(),
                p.getNumber(),
                p.getSize(),
                p.getTotalElements());
    }
}
