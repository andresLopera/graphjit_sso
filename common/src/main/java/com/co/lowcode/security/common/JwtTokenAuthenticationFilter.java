package com.co.lowcode.security.common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.co.lowcode.lineabase.model.Endpoint;
import com.co.lowcode.lineabase.model.MicroService;
import com.co.lowcode.service.RedisService;


public class JwtTokenAuthenticationFilter extends OncePerRequestFilter {

    private final JwtAuthenticationConfig config;
    
    private RedisService redisService;

    public JwtTokenAuthenticationFilter(JwtAuthenticationConfig config, RedisService redisService) {
        this.config = config;
        this.redisService = redisService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse rsp, FilterChain filterChain)
            throws ServletException, IOException {
    	
    	
    	
    	
        String token = req.getHeader(config.getHeader());
        if (token != null && token.startsWith(config.getPrefix() + " ")) {
            token = token.replace(config.getPrefix() + " ", "");
            try {
                Claims claims = Jwts.parser()
                        .setSigningKey(config.getSecret().getBytes())
                        .parseClaimsJws(token)
                        .getBody();
                String username = claims.getSubject();
                @SuppressWarnings("unchecked")
                List<String> authorities = claims.get("authorities", List.class);
                
                if (username != null) {
                	
                	ObjectMapper mapper = new ObjectMapper();
            	    CollectionType listType = mapper.getTypeFactory().constructCollectionType(ArrayList.class, Endpoint.class);
            	    List<Endpoint> endpoints = null ;
                	if( claims.get("uuid") != null) {
                		String uuid = claims.get("uuid").toString();
                		String json =  (String) redisService.getValue(String.format("%s:%s", username, uuid));
                		endpoints = mapper.readValue(json, listType);
                	}

                	List<String> jsonString = claims.get("endpoints", List.class);
                	 
                	
                	if(jsonString != null) {
                		mapper = new ObjectMapper();
                    	endpoints = mapper.convertValue(jsonString, new TypeReference<List<Endpoint>>() { });
                	}
                	
                	
                	boolean sw = false;
                	for(Endpoint e : endpoints) {
                		if(e.getNumberParams() == null) {
                			e.setNumberParams(0);
                		}
                		if(req.getMethod().equals(e.getMethod()) && 
                				req.getRequestURI().replaceAll("//", "/").equals(
                						((MicroService)e.getMicroservice().toArray()[0]).getRequestURI() + "" +  e.getPath()) &&
                				req.getParameterMap().size() == e.getNumberParams()){
                			sw = true;
                			break;
                		}
                	}
                	if(sw) {
                		UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(username, null,
                                authorities.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList()));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                	}
                    
                }
              
            } catch (Exception ignore) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(req, rsp);
    }
}
