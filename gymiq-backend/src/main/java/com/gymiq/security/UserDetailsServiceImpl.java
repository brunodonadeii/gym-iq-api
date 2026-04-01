package com.gymiq.security;

import com.gymiq.entity.Usuario;
import com.gymiq.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuário não encontrado com e-mail: " + email));

        if (!usuario.getAtivo()) {
            throw new UsernameNotFoundException("Usuário inativo: " + email);
        }

        return User.builder()
                .username(usuario.getEmail())
                .password(usuario.getSenhaHash())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getPerfil().name())))
                .build();
    }
}
