package br.unisc.psagestor.arquivos.repository;

import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class CroquiConsultaRepository extends br.unisc.psagestor.comum.repository.ProjecaoNativa {

    public CroquiConsultaRepository() {}

    public Object listar(long id, int page, int size) {
        return pagina(
                List.of(
                        campo("id"),
                        campo("propriedade_id"),
                        campo("nome_original"),
                        campo("tipo_mime"),
                        campo("tamanho"),
                        campo("area_psa"),
                        campo("observacoes"),
                        dataHora("criado_em")),
                "SELECT id,propriedade_id,nome_original,tipo_mime,tamanho,area_psa,observacoes,criado_em FROM croqui WHERE propriedade_id=?",
                page,
                size,
                id);
    }
}
