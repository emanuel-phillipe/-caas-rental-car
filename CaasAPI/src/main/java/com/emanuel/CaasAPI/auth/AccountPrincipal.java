package com.emanuel.CaasAPI.auth;

import java.util.UUID;

import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;

public final class AccountPrincipal extends User {

	private final UUID id;
	private final String name;
	private final Role role;

	AccountPrincipal(UUID id, String username, String name, String passwordHash, Role role, boolean enabled) {
		super(username, passwordHash, enabled, true, true, true, AuthorityUtils.createAuthorityList("ROLE_" + role.name()));
		this.id = id;
		this.name = name;
		this.role = role;
	}

	public UUID getId() { return id; }

	Profile profile() {
		return new Profile(id, getUsername(), name, role);
	}

	enum Role {
		CLIENTE, AGENTE
	}

	record Profile(UUID id, String username, String name, Role role) {}

}
