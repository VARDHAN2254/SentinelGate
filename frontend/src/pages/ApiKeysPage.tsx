import React, { useEffect, useState } from 'react';
import { Key, Plus, Ban, Copy, Check, RefreshCw } from 'lucide-react';
import axios from 'axios';

interface ApiKey {
  id: number;
  name: string;
  keyPrefix: string;
  ownerUsername: string;
  status: string;
  rateLimitPerMin: number;
  lastUsedAt?: string;
  createdAt: string;
}

export const ApiKeysPage: React.FC = () => {
  const [keys, setKeys] = useState<ApiKey[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [showModal, setShowModal] = useState<boolean>(false);
  const [keyName, setKeyName] = useState<string>('');
  const [rateLimit, setRateLimit] = useState<number>(1000);
  const [rawKeyResponse, setRawKeyResponse] = useState<string | null>(null);
  const [copied, setCopied] = useState<boolean>(false);

  const fetchKeys = async () => {
    setLoading(true);
    try {
      const res = await axios.get('/api/v1/admin/api-keys').catch(() => ({ data: [] }));
      setKeys(res.data || []);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchKeys();
  }, []);

  const handleGenerateKey = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const res = await axios.post('/api/v1/admin/api-keys', {
        name: keyName,
        rateLimitPerMin: rateLimit,
      });
      setRawKeyResponse(res.data.rawKey);
      fetchKeys();
    } catch (err) {
      alert('Error generating API Key');
    }
  };

  const handleRevokeKey = async (id: number) => {
    if (confirm('Are you sure you want to revoke this API Key? Machine clients using this key will immediately lose access.')) {
      await axios.post(`/api/v1/admin/api-keys/${id}/revoke`);
      fetchKeys();
    }
  };

  const copyToClipboard = () => {
    if (rawKeyResponse) {
      navigator.clipboard.writeText(rawKeyResponse);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  return (
    <div className="p-6 space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-slate-900 border border-slate-800 p-4 rounded-xl">
        <div>
          <h2 className="text-xl font-bold text-slate-100 flex items-center gap-2">
            <Key className="w-5 h-5 text-cyan-400" />
            Machine-to-Machine API Keys
          </h2>
          <p className="text-sm text-slate-400">Issue, monitor, and revoke high-performance client credentials</p>
        </div>

        <div className="flex items-center gap-3">
          <button 
            onClick={fetchKeys}
            className="flex items-center gap-2 px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-medium rounded-lg transition-colors border border-slate-700"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
            Refresh
          </button>
          <button
            onClick={() => {
              setRawKeyResponse(null);
              setShowModal(true);
            }}
            className="flex items-center gap-2 px-4 py-2 bg-cyan-600 hover:bg-cyan-500 text-white text-xs font-bold rounded-lg transition-all shadow-lg shadow-cyan-600/20"
          >
            <Plus className="w-4 h-4" />
            Generate API Key
          </button>
        </div>
      </div>

      {/* Keys Table */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs text-slate-300">
            <thead className="bg-slate-950/80 uppercase text-[11px] font-semibold text-slate-400 border-b border-slate-800">
              <tr>
                <th className="p-4">Key Name</th>
                <th className="p-4">Key Identifier Prefix</th>
                <th className="p-4">Owner</th>
                <th className="p-4">Rate Limit</th>
                <th className="p-4">Last Used</th>
                <th className="p-4">Status</th>
                <th className="p-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 font-mono">
              {keys.length === 0 && (
                <tr>
                  <td colSpan={7} className="p-8 text-center text-slate-500 font-sans">
                    No machine API keys generated yet.
                  </td>
                </tr>
              )}
              {keys.map((k) => (
                <tr key={k.id} className="hover:bg-slate-800/30 transition-colors">
                  <td className="p-4 font-sans font-semibold text-slate-100">{k.name}</td>
                  <td className="p-4 text-cyan-400 font-bold">{k.keyPrefix}...</td>
                  <td className="p-4 font-sans text-slate-300">{k.ownerUsername}</td>
                  <td className="p-4 text-slate-300">{k.rateLimitPerMin} req/min</td>
                  <td className="p-4 text-slate-400 font-sans text-[11px]">
                    {k.lastUsedAt ? new Date(k.lastUsedAt).toLocaleString() : 'Never'}
                  </td>
                  <td className="p-4 font-sans">
                    <span className={`px-2 py-0.5 rounded text-[10px] font-bold border ${
                      k.status === 'ACTIVE'
                        ? 'bg-emerald-950 text-emerald-400 border-emerald-800'
                        : 'bg-rose-950 text-rose-400 border-rose-800'
                    }`}>
                      {k.status}
                    </span>
                  </td>
                  <td className="p-4 text-right">
                    {k.status === 'ACTIVE' && (
                      <button 
                        onClick={() => handleRevokeKey(k.id)}
                        className="px-2.5 py-1 bg-rose-950/60 hover:bg-rose-900 text-rose-400 border border-rose-800 rounded font-sans font-semibold text-[11px] transition-colors flex items-center gap-1 ml-auto"
                      >
                        <Ban className="w-3.5 h-3.5" /> Revoke
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Generator Modal */}
      {showModal && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-xl max-w-md w-full p-6 space-y-4 shadow-2xl">
            <h3 className="text-lg font-bold text-slate-100 flex items-center gap-2">
              <Key className="w-5 h-5 text-cyan-400" />
              Generate Machine API Key
            </h3>

            {!rawKeyResponse ? (
              <form onSubmit={handleGenerateKey} className="space-y-3 text-xs">
                <div>
                  <label className="block text-slate-400 mb-1 font-semibold">Key Name / Description</label>
                  <input 
                    type="text" 
                    required
                    placeholder="e.g. Payments Ingestion Worker"
                    value={keyName}
                    onChange={(e) => setKeyName(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg p-2.5 text-slate-100 focus:outline-none focus:border-cyan-500"
                  />
                </div>

                <div>
                  <label className="block text-slate-400 mb-1 font-semibold">Rate Limit Threshold (req/min)</label>
                  <input 
                    type="number" 
                    required
                    value={rateLimit}
                    onChange={(e) => setRateLimit(Number(e.target.value))}
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg p-2.5 text-slate-100 focus:outline-none focus:border-cyan-500 font-mono"
                  />
                </div>

                <div className="flex justify-end gap-3 pt-4 border-t border-slate-800">
                  <button
                    type="button"
                    onClick={() => setShowModal(false)}
                    className="px-4 py-2 bg-slate-800 text-slate-300 rounded-lg font-medium hover:bg-slate-700 transition-colors"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="px-4 py-2 bg-cyan-600 hover:bg-cyan-500 text-white rounded-lg font-bold transition-all shadow-lg shadow-cyan-600/20"
                  >
                    Generate Key
                  </button>
                </div>
              </form>
            ) : (
              <div className="space-y-4 text-xs">
                <div className="p-3 bg-amber-950/40 border border-amber-800/80 rounded-lg text-amber-300 text-[11px] leading-relaxed">
                  ⚠️ <strong>IMPORTANT:</strong> Save this raw API secret key now. It will <u>NEVER</u> be displayed again.
                </div>

                <div className="bg-slate-950 border border-slate-800 rounded-lg p-3 flex items-center justify-between font-mono text-cyan-400 break-all select-all">
                  <span>{rawKeyResponse}</span>
                  <button 
                    onClick={copyToClipboard}
                    className="p-1.5 hover:bg-slate-800 rounded text-slate-300 transition-colors shrink-0 ml-2"
                  >
                    {copied ? <Check className="w-4 h-4 text-emerald-400" /> : <Copy className="w-4 h-4" />}
                  </button>
                </div>

                <div className="pt-2 flex justify-end">
                  <button
                    onClick={() => setShowModal(false)}
                    className="px-4 py-2 bg-slate-800 text-slate-200 hover:bg-slate-700 rounded-lg font-bold transition-colors"
                  >
                    Done & Close
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};
