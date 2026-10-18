package br.unisc.psagestor.acesso.api;

import br.unisc.psagestor.comum.api.ErroApi;
import br.unisc.psagestor.comum.api.RequisicaoIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import tools.jackson.databind.json.JsonMapper;

public class RespostaSeguranca {
    private final JsonMapper mapper;

    public RespostaSeguranca(JsonMapper mapper) {
        this.mapper = mapper;
    }

    public void escrever(
            HttpServletRequest request,
            HttpServletResponse response,
            int status,
            String codigo,
            String mensagem)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter()
                .write(
                        mapper.writeValueAsString(
                                ErroApi.de(
                                        codigo,
                                        mensagem,
                                        (String)
                                                request.getAttribute(
                                                        RequisicaoIdFilter.ATRIBUTO))));
    }
}
