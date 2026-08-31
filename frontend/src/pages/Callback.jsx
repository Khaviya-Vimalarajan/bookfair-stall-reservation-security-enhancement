import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

// Global reference to store the active callback promise across component remounts in StrictMode
let callbackPromise = null;

export default function Callback() {
  const { handleCallback } = useAuth();
  const navigate = useNavigate();
  const [error, setError] = useState('');

  useEffect(() => {
    if (!callbackPromise) {
      callbackPromise = handleCallback();
    }

    callbackPromise
      .then(() => {
        // Reset the promise ref on success to allow subsequent logins in the same session
        callbackPromise = null;
        navigate('/', { replace: true });
      })
      .catch((err) => {
        // Reset the promise ref on error to allow retries
        callbackPromise = null;
        console.error('OIDC Callback error:', err);
        setError('Authentication failed. Please try again.');
      });
  }, [handleCallback, navigate]);

  if (error) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-gray-900 text-red-400 p-6">
        <h1 className="text-2xl font-bold mb-4">Error</h1>

        <p>{error}</p>

        <button
          onClick={() => navigate('/login')}
          className="mt-4 px-4 py-2 bg-amber-500 text-stone-900 rounded-lg font-semibold"
        >
          Back to Login
        </button>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-900 text-gray-100">
      <div className="flex flex-col items-center gap-4">
        <div className="w-12 h-12 border-4 border-amber-400 border-t-transparent rounded-full animate-spin"></div>

        <p className="text-lg">
          Completing login. Please wait...
        </p>
      </div>
    </div>
  );
}