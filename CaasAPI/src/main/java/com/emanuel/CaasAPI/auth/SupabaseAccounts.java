package com.emanuel.CaasAPI.auth;

import com.emanuel.CaasAPI.database.SupabaseConnection;
import java.sql.SQLException;
import java.util.Locale;
import java.util.UUID;

import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

final class SupabaseAccounts implements UserDetailsService {

	private static final String FIND_ACCOUNT = """
			SELECT id, username, display_name, password_hash, role, enabled
			FROM caas_private.accounts
			WHERE username = ?
			""";

	private final SupabaseConnection database;

	SupabaseAccounts(SupabaseConnection database) {
		this.database = database;
	}

	@Override
	public AccountPrincipal loadUserByUsername(String username) {
		String normalized = username.strip().toLowerCase(Locale.ROOT);
		if (normalized.isEmpty() || normalized.length() > 64) {
			throw new UsernameNotFoundException("Conta não encontrada");
		}

		try (var connection = database.open();
				var statement = connection.prepareStatement(FIND_ACCOUNT)) {
			statement.setString(1, normalized);
			try (var result = statement.executeQuery()) {
				if (result.next()) {
					return new AccountPrincipal(result.getObject("id", UUID.class), result.getString("username"),
							result.getString("display_name"), result.getString("password_hash"),
							AccountPrincipal.Role.valueOf(result.getString("role")), result.getBoolean("enabled"));
				}
			}
		}
		catch (SQLException exception) {
			throw new InternalAuthenticationServiceException("Falha ao consultar as contas no Supabase", exception);
		}
		throw new UsernameNotFoundException("Conta não encontrada");
	}

}
