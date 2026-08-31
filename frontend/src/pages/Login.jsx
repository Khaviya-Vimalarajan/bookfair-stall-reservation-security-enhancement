import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Login() {
  const { login, isLoggedIn, isAdmin } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (isLoggedIn) {
      navigate(isAdmin ? '/admin' : '/');
    }
  }, [isLoggedIn, isAdmin, navigate]);

  const handleSsoLogin = () => {
    login().catch((err) => {
      console.error('SSO login failed:', err);
    });
  };

  return (
    <div className="container mx-auto px-4 py-12 max-w-md text-center">
      <div className="bg-slate-800 rounded-xl shadow-lg border border-stone-700 p-8 animate-fadeIn">
        <h1 className="font-display text-3xl font-bold mb-6 text-blue-300">Welcome</h1>
        <p className="text-stone-300 mb-8 text-sm">
          Please authenticate using our secure Single Sign-On (SSO) server.
        </p>
        <button
          onClick={handleSsoLogin}
          className="w-full bg-amber-500 hover:bg-amber-600 text-stone-900 font-bold py-3 px-6 rounded-lg transition duration-200"
        >
          Sign In with OIDC
        </button>
      </div>
    </div>
  );
}