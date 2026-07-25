package com.castlecsr.security;

import com.castlecsr.model.Usuario;
import com.castlecsr.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void loadUserByUsername_conUsuarioExistente_devuelveUserDetailsConRolCorrecto() {
        Usuario usuario = new Usuario("jgomez", "hashDePassword", "ADMIN");
        when(usuarioRepository.findByUsername("jgomez")).thenReturn(Optional.of(usuario));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("jgomez");

        assertEquals("jgomez", userDetails.getUsername());
        assertEquals("hashDePassword", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void loadUserByUsername_conUsuarioInexistente_lanzaUsernameNotFoundException() {
        when(usuarioRepository.findByUsername("no_existe")).thenReturn(Optional.empty());

        assertThrows(
                UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername("no_existe")
        );
    }

    @Test
    void loadUsuarioEntity_conUsuarioExistente_devuelveLaEntidadCompleta() {
        Usuario usuario = new Usuario("jgomez", "hashDePassword", "ADMIN");
        when(usuarioRepository.findByUsername("jgomez")).thenReturn(Optional.of(usuario));

        Usuario resultado = customUserDetailsService.loadUsuarioEntity("jgomez");

        assertEquals(usuario, resultado);
    }

    @Test
    void loadUsuarioEntity_conUsuarioInexistente_lanzaUsernameNotFoundException() {
        when(usuarioRepository.findByUsername("no_existe")).thenReturn(Optional.empty());

        assertThrows(
                UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUsuarioEntity("no_existe")
        );
    }
}