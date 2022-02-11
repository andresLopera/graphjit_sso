package com.co.lowcode.lineabase.oauth.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.co.lowcode.lineabase.model.Role;
import com.co.lowcode.lineabase.repository.RoleRepository;

@Service
public class RoleService {
	@Autowired
	RoleRepository roleRepository;
	
	public Iterable<Role> findAll() {
		return roleRepository.findAll();
	}
	
	public Role save(Role role){
		Role roleCreated = roleRepository.save(role);
		return roleCreated;
	}
	
	public Role getByName(String name){
		return roleRepository.findByName(name);
	}
}