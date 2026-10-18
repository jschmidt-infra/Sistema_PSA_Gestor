package br.unisc.psagestor.acesso.api;

import br.unisc.psagestor.acesso.api.UsuariosEntradas.Criar;
import br.unisc.psagestor.acesso.api.UsuariosEntradas.Entrada;
import br.unisc.psagestor.acesso.api.UsuariosEntradas.Senha;
import br.unisc.psagestor.acesso.service.AdministracaoUsuarioService;
import br.unisc.psagestor.acesso.service.LimiteLogin;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/usuarios")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class UsuariosController {
    private final AdministracaoUsuarioService service;
    private final LimiteLogin limite;

    public UsuariosController(AdministracaoUsuarioService service, LimiteLogin limite) {
        this.service = service;
        this.limite = limite;
    }

    @GetMapping
    public Object listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listar(page, size);
    }

    @PostMapping
    public Object criar(@Valid @RequestBody Criar entrada) {
        var usuario = service.criar(entrada);
        return ResponseEntity.created(URI.create("/api/v1/usuarios/" + usuario.get("id")))
                .body(usuario);
    }

    @PutMapping("/{id}")
    public Object editar(@PathVariable long id, @Valid @RequestBody Entrada entrada) {
        return service.editar(id, entrada);
    }

    @PutMapping("/{id}/senha")
    public ResponseEntity<Void> senha(@PathVariable long id, @Valid @RequestBody Senha entrada) {
        service.senha(id, entrada);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/bloqueio-login")
    public Object bloqueio(@PathVariable long id) {
        return limite.consultar(id);
    }

    @DeleteMapping("/{id}/bloqueio-login")
    public ResponseEntity<Void> desbloquear(@PathVariable long id) {
        limite.desbloquear(id);
        return ResponseEntity.noContent().build();
    }
}
