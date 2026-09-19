import React, { createContext, useContext, useState, useEffect, useCallback, type ReactNode } from 'react';
import type { User, DemoPersona } from '../types';
import { authApi } from '../api/auth';
import {
  ACCESS_TOKEN_KEY,
  REFRESH_TOKEN_KEY,
  CURRENT_USER_KEY,
} from '../api/client';

export const DEMO_PERSONAS: DemoPersona[] = [
  {
    key: 'dispatcher',
    username: 'dispatcher',
    displayName: 'Sarah Jenkins',
    title: 'Lead Operations Dispatcher',
    role: 'ROLE_DISPATCHER',
    avatarColor: 'bg-emerald-600',
  },
  {
    key: 'tech_dave',
    username: 'tech_dave',
    displayName: 'Dave Miller',
    title: 'HVAC Specialist',
    role: 'ROLE_TECHNICIAN',
    avatarColor: 'bg-blue-600',
  },
  {
    key: 'admin',
    username: 'admin',
    displayName: 'Alex Vance',
    title: 'System Administrator',
    role: 'ROLE_ADMIN',
    avatarColor: 'bg-purple-600',
  },
];

const DEFAULT_DEMO_PASSWORD = 'password123';

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  activePersona: DemoPersona | null;
  demoPersonas: DemoPersona[];
  login: (username: string, password?: string) => Promise<void>;
  switchPersona: (personaKey: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  // Derive active persona if user matches one of the demo accounts
  const activePersona = user
    ? DEMO_PERSONAS.find((p) => p.username === user.username) || null
    : null;

  const handleAuthSuccess = useCallback((accessToken: string, refreshToken: string, userData: User) => {
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
    localStorage.setItem(CURRENT_USER_KEY, JSON.stringify(userData));
    setUser(userData);
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(CURRENT_USER_KEY);
    setUser(null);
  }, []);

  const login = useCallback(async (username: string, password = DEFAULT_DEMO_PASSWORD) => {
    setIsLoading(true);
    try {
      const authResponse = await authApi.login(username, password);
      handleAuthSuccess(
        authResponse.accessToken,
        authResponse.refreshToken,
        authResponse.user
      );
    } finally {
      setIsLoading(false);
    }
  }, [handleAuthSuccess]);

  const switchPersona = async (personaKey: string) => {
    const persona = DEMO_PERSONAS.find((p) => p.key === personaKey);
    if (!persona) return;
    await login(persona.username, DEFAULT_DEMO_PASSWORD);
  };

  // Re-hydrate session on mount
  useEffect(() => {
    const initAuth = async () => {
      const storedToken = localStorage.getItem(ACCESS_TOKEN_KEY);
      const storedUser = localStorage.getItem(CURRENT_USER_KEY);

      if (storedToken && storedUser) {
        try {
          setUser(JSON.parse(storedUser));
          // Verify token validity with backend
          const freshUser = await authApi.getMe();
          setUser(freshUser);
          localStorage.setItem(CURRENT_USER_KEY, JSON.stringify(freshUser));
        } catch {
          // Token invalid or expired, attempt fallback login as default dispatcher for demo continuity
          try {
            await login('dispatcher', DEFAULT_DEMO_PASSWORD);
          } catch {
            logout();
          }
        }
      } else {
        // Auto-login default dispatcher for instant demo readiness
        try {
          await login('dispatcher', DEFAULT_DEMO_PASSWORD);
        } catch {
          setIsLoading(false);
        }
      }
      setIsLoading(false);
    };

    initAuth();
  }, [login, logout]);

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        isLoading,
        activePersona,
        demoPersonas: DEMO_PERSONAS,
        login,
        switchPersona,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
