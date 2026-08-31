import { useEffect, useState } from 'react';
import { profileApi } from '../api/client';

export default function Profile() {
  const [profile, setProfile] = useState(null);
  const [form, setForm] = useState({ name: '', email: '', phone: '', businessName: '' });
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    profileApi.get().then((p) => {
      setProfile(p);
      setForm({ name: p.name || '', email: p.email || '', phone: p.phone || '', businessName: p.businessName || '' });
    }).catch(() => setProfile(null)).finally(() => setLoading(false));
  }, []);

  const handleSubmit = (e) => {
    e.preventDefault();
    setMessage('');
    profileApi.update(form).then(() => setMessage('Profile updated successfully.')).catch((err) => setMessage(err.message || 'Update failed'));
  };

  if (loading) return <div className="container mx-auto px-4 py-12 text-gray-100">Loading...</div>;
  if (!profile) return <div className="container mx-auto px-4 py-12 text-gray-100">Profile not found.</div>;

  return (
    <div className="container mx-auto px-4 py-12 max-w-md text-gray-100">
      <h1 className="font-display text-3xl font-bold mb-6 text-blue-300">My Profile</h1>
      <form onSubmit={handleSubmit} className="space-y-4 bg-slate-800 p-6 rounded-xl border border-stone-700 shadow-md">
        <div>
          <label className="block font-medium mb-1 text-stone-300 text-sm">Full Name</label>
          <input
            type="text"
            value={form.name}
            onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
            className="w-full border border-stone-600 rounded-lg p-2 bg-slate-700 text-white focus:ring-amber-500 focus:border-amber-500"
          />
        </div>
        <div>
          <label className="block font-medium mb-1 text-stone-300 text-sm">Email Address (Identity - Read Only)</label>
          <input
            type="email"
            value={form.email}
            readOnly
            className="w-full border border-stone-600 rounded-lg p-2 bg-slate-800 text-stone-400 cursor-not-allowed"
          />
        </div>
        <div>
          <label className="block font-medium mb-1 text-stone-300 text-sm">Contact Number</label>
          <input
            type="text"
            value={form.phone}
            onChange={(e) => setForm((f) => ({ ...f, phone: e.target.value }))}
            className="w-full border border-stone-600 rounded-lg p-2 bg-slate-700 text-white focus:ring-amber-500 focus:border-amber-500"
            placeholder="Enter contact number"
          />
        </div>
        <div>
          <label className="block font-medium mb-1 text-stone-300 text-sm">Organization / Business Name</label>
          <input
            type="text"
            value={form.businessName}
            onChange={(e) => setForm((f) => ({ ...f, businessName: e.target.value }))}
            className="w-full border border-stone-600 rounded-lg p-2 bg-slate-700 text-white focus:ring-amber-500 focus:border-amber-500"
            placeholder="Enter business name"
          />
        </div>
        {message && <p className={message.startsWith('Profile updated') ? 'text-green-400' : 'text-red-400'}>{message}</p>}
        <button type="submit" className="w-full bg-amber-500 hover:bg-amber-600 text-stone-900 font-semibold py-3 rounded-lg transition duration-200">
          Save Changes
        </button>
      </form>
    </div>
  );
}