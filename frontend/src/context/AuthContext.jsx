import { createContext, useContext, useState, useEffect } from 'react';
import { UserManager, WebStorageStateStore } from 'oidc-client-ts';

const AuthContext = createContext(null);

const oidcConfig = {
  authority: import.meta.env.VITE_OIDC_AUTHORITY || import.meta.env.VITE_OIDC_ISSUER || 'https://mock-issuer.local',
  client_id: import.meta.env.VITE_OIDC_CLIENT_ID || 'stall-reservation-client',
  redirect_uri: import.meta.env.VITE_OIDC_REDIRECT_URI || `${window.location.origin}/callback`,
  post_logout_redirect_uri: import.meta.env.VITE_OIDC_POST_LOGOUT_REDIRECT_URI || window.location.origin,
  response_type: 'code',
  scope: import.meta.env.VITE_OIDC_SCOPE || 'openid profile email roles',
  extraQueryParams: {
    audience: import.meta.env.VITE_OIDC_AUDIENCE
  },
  userStore: new WebStorageStateStore({ store: window.sessionStorage })
};

const userManager = new UserManager(oidcConfig);

export function AuthProvider({ children }) {
  const [tokenUser, setTokenUser] = useState(null);
  const [token, setToken] = useState(() => sessionStorage.getItem('token'));
  const [loading, setLoading] = useState(true);

  // Initialize and check current user state
  useEffect(() => {
    userManager.getUser()
      .then((user) => {
        if (user && !user.expired) {
          setToken(user.access_token);
          setTokenUser(user.profile);
          sessionStorage.setItem('token', user.access_token);
        } else {
          setToken(null);
          setTokenUser(null);
          sessionStorage.removeItem('token');
        }
      })
      .catch(() => {
        setToken(null);
        setTokenUser(null);
      })
      .finally(() => setLoading(false));
  }, []);

  const login = () => {
    return userManager.signinRedirect();
  };

  const logout = () => {
    sessionStorage.removeItem('token')
    setToken(null);
    setTokenUser(null);
    return userManager.signoutRedirect().catch(() => {
      // Fallback local signout if IdP endpoint fails/is unconfigured
      userManager.clearStaleState();
      window.location.href = oidcConfig.post_logout_redirect_uri;
    });
  };

  const handleCallback = async () => {
    const user = await userManager.signinRedirectCallback();
    if (user) {
      setToken(user.access_token);
      setTokenUser(user.profile);
      sessionStorage.setItem('token', user.access_token);
    }
    return user;
  };

  // Roles check based on custom claims mapped from identity provider
  // Standard claims could list roles under 'roles' or 'groups'
const roles =
  tokenUser?.['https://bookfair-app/roles'] || [];

const isAdmin =
  roles.includes('EXHIBITION_ORGANIZER');

const isVendor =
  roles.includes('STALL_VENDOR');

  const contextValue = {
    user: tokenUser ? {
      sub: tokenUser.sub,
      name: tokenUser.name || tokenUser.preferred_username || tokenUser.email,
      email: tokenUser.email,
      phone: tokenUser.phone_number || '',
      role: isAdmin ? 'EXHIBITION_ORGANIZER' : 'STALL_VENDOR'
    } : null,
    token,
    login,
    logout,
    handleCallback,
    isAdmin,
    isVendor,
    isLoggedIn: !!tokenUser,
    loading
  };

  return (
    <AuthContext.Provider value={contextValue}>
      {!loading && children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}