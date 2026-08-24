import React, { useEffect, useState } from 'react';
import { Sliders, RefreshCw, ShieldAlert, CheckCircle2 } from 'lucide-react';
import axios from 'axios';

interface SecurityRule {
  id: number;
  ruleName: string;
  ruleType: string;
  thresholdCount: number;
  windowSeconds: number;
  actionTaken: string;
  enabled: boolean;
}

const DEFAULT_POLICIES = [
  {
    name: 'IP Rate Limiting',
    description: 'Limits total request volume per client IP address per minute using Redis sliding windows.',
    threshold: '100 req',
    window: '60 seconds',
    action: 'HTTP 429 Too Many Requests',
    status: 'ACTIVE'
  },
  {
    name: 'Brute-Force Auth Detection',
    description: 'Monitors repeated failed login attempts from a single source IP to detect credential stuffing.',
    threshold: '5 failures',
    window: '300 seconds',
    action: 'BRUTE_FORCE Security Event',
    status: 'ACTIVE'
  },
  {
    name: 'API Key Machine Throttling',
    description: 'Protects backend microservices from machine-to-machine client API key floods.',
    threshold: '1000 req',
    window: '60 seconds',
    action: 'Throttle & Audit Log',
    status: 'ACTIVE'
  }
];

export const SecurityRulesPage: React.FC = () => {
  const [rules, setRules] = useState<SecurityRule[]>([]);
  const [loading, setLoading] = useState<boolean>(true);

  const fetchRules = async () => {
    setLoading(true);
    try {
      const res = await axios.get('/api/v1/admin/security-rules').catch(() => ({ data: [] }));
      setRules(res.data || []);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRules();
  }, []);

  return (
    <div className="p-6 space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-slate-900 border border-slate-800 p-4 rounded-xl">
        <div>
          <h2 className="text-xl font-bold text-slate-100 flex items-center gap-2">
            <Sliders className="w-5 h-5 text-cyan-400" />
            Security Rule Policies
          </h2>
          <p className="text-sm text-slate-400">Configure automated defensive threshold triggers and security engine rules</p>
        </div>

        <button 
          onClick={fetchRules}
          className="flex items-center gap-2 px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-medium rounded-lg transition-colors border border-slate-700"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          Refresh Policies
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {rules.length > 0 ? (
          rules.map((rule) => (
            <div key={rule.id} className="bg-slate-900 border border-slate-800 p-5 rounded-xl space-y-3">
              <div className="flex items-center justify-between">
                <h3 className="font-bold text-slate-100 text-sm flex items-center gap-2">
                  <ShieldAlert className="w-4 h-4 text-cyan-400" />
                  {rule.ruleName}
                </h3>
                <span className={`px-2 py-0.5 rounded text-[10px] font-mono border ${
                  rule.enabled 
                    ? 'bg-cyan-950 text-cyan-400 border-cyan-800'
                    : 'bg-slate-800 text-slate-400 border-slate-700'
                }`}>
                  {rule.enabled ? 'ACTIVE' : 'DISABLED'}
                </span>
              </div>
              <p className="text-xs text-slate-400 leading-relaxed font-mono">{rule.ruleType}</p>
              <div className="bg-slate-950 p-3 rounded-lg text-xs font-mono text-slate-300 space-y-1">
                <div>Threshold: <span className="text-cyan-400">{rule.thresholdCount}</span></div>
                <div>Window: <span className="text-cyan-400">{rule.windowSeconds} seconds</span></div>
                <div>Action: <span className="text-rose-400">{rule.actionTaken}</span></div>
              </div>
            </div>
          ))
        ) : (
          DEFAULT_POLICIES.map((policy, idx) => (
            <div key={idx} className="bg-slate-900 border border-slate-800 p-5 rounded-xl space-y-3">
              <div className="flex items-center justify-between">
                <h3 className="font-bold text-slate-100 text-sm flex items-center gap-2">
                  <CheckCircle2 className="w-4 h-4 text-cyan-400" />
                  {policy.name}
                </h3>
                <span className="px-2 py-0.5 rounded bg-cyan-950 text-cyan-400 border border-cyan-800 text-[10px] font-mono">
                  {policy.status}
                </span>
              </div>
              <p className="text-xs text-slate-400 leading-relaxed">{policy.description}</p>
              <div className="bg-slate-950 p-3 rounded-lg text-xs font-mono text-slate-300 space-y-1">
                <div>Threshold: <span className="text-cyan-400">{policy.threshold}</span></div>
                <div>Window: <span className="text-cyan-400">{policy.window}</span></div>
                <div>Action: <span className="text-rose-400">{policy.action}</span></div>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};
