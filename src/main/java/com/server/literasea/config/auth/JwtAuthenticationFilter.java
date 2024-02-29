package com.server.literasea.config.auth;

import com.server.literasea.entity.Users;
import com.server.literasea.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.NullAuthoritiesMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String[] NO_CHECK_URI_ARRAY = {
            //토큰 필요 없는 것들은 여기에서 설정해야 함
            "api/v1/user/**",
            "/swagger-ui/index.html",
            "/favicon.ico"
    };

    private static final List<String> NO_CHECK_URIS = new ArrayList<>(
            Arrays.asList(NO_CHECK_URI_ARRAY));
    private final JwtService jwtService;
    private final UserRepository userRepository;

    private GrantedAuthoritiesMapper authoritiesMapper = new NullAuthoritiesMapper();
    

    public void checkAccessTokenAndAuthentication(HttpServletRequest request,
                                                  HttpServletResponse response,
                                                  FilterChain filterChain) throws ServletException, IOException {
        log.info("checkAccessTokenAndAuthentication() 호출");
        //요청오는 도메인이 saveWord인지 확인
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        boolean isPostWordRequest = "POST".equalsIgnoreCase(method) && requestURI.startsWith("/api/v1/word/");
        //확인완료
        jwtService.extractAccessToken(request)
                .filter(jwtService::isTokenValid)
                .ifPresent(accessToken -> jwtService.extractUuid(accessToken)
                        .ifPresent(id -> {
                            if (isPostWordRequest) {  //도메인이 saveWord면
                                userRepository.findByEmailWithJPQL(id)
                                        .ifPresent(this::saveAuthentication);
                            } else  //saveWord 요청이 아닐시 원래대로 실행
                            userRepository.findByEmail(id)
                                    .ifPresent(this::saveAuthentication);
                        }));
        filterChain.doFilter(request, response);
        jwtService.extractAccessToken(request)
                .filter(jwtService::isTokenValid)
                .ifPresent(accessToken -> jwtService.extractUuid(accessToken)
                        .ifPresent(id -> userRepository.findByEmail(id)
                                .ifPresent(this::saveAuthentication)));

        filterChain.doFilter(request, response);




    }



    public void saveAuthentication(Users users) {

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(users, null,
                        authoritiesMapper.mapAuthorities(users.getAuthorities()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (request.getRequestURI().equals(NO_CHECK_URIS)) {
            log.info("로그인 진입");
            filterChain.doFilter(request, response);
            return; // return으로 이후 현재 필터 진행 막기 (안해주면 아래로 내려가서 계속 필터 진행시킴)
        }

        log.info("로그인 이외 페이지 진입");

        checkAccessTokenAndAuthentication(request, response, filterChain);

    }
}
