import React, { useEffect, useState } from 'react';
import { Shield, Bell, Server, Database, Zap } from 'lucide-react';
import axios from 'axios';

interface SystemHealth {
  status: string;
  components: {
    redis: string;
    database: string;
    gateway: string;
  };
}

export const Header: React.FC = () => {
  const [health, setHealth] = useState<SystemHealth | null>(null);

  useEffect(() => {
    const fetchHealth = async () => {
      try {
        const response = await axios.get('/api/v1/system/health');
        setHealth(response.data);
      } catch (error) {
        setHealth({
          status: 'DOWN',
          components: { redis: 'UNKNOWN', database: 'UNKNOWN', gateway: 'DOWN' }
        });
      }
    };

    fetchHealth();
    const interval = setInterval(fetchHealth, 10000);
    return () => clearInterval(interval);
  }, []);

  return (
    <header className="h-16 bg-slate-900 border-b border-slate-800 px-6 flex items-center justify-between sticky top-0 z-40">
      {/* Left: Brand */}
      <div className="flex items-center gap-3">
        <div className="p-2 bg-cyan-500/10 rounded-lg border border-cyan-500/30 text-cyan-400">
          <Shield className="w-6 h-6" />
        </div>
        <div>
          <h1 className="text-lg font-bold text-slate-100 flex items-center gap-2">
            SentinelGate
            <span className="text-xs px-2 py-0.5 rounded bg-cyan-950 text-cyan-400 border border-cyan-800 font-mono">
              v1.0.0
            </span>
          </h1>
          <p className="text-xs text-slate-400">Secure API Gateway & Analytics</p>
        </div>
      </div>

      {/* Right: Component Health Badges & Actions */}
      <div className="flex items-center gap-4">
        <div className="flex items-center gap-2 bg-slate-950/80 px-3 py-1.5 rounded-lg border border-slate-800 text-xs font-mono">
          <div className="flex items-center gap-1.5 text-slate-300">
            <Zap className="w-3.5 h-3.5 text-cyan-400" />
            <span>Gateway:</span>
            <span className={`font-semibold ${health?.components?.gateway === 'UP' ? 'text-emerald-400' : 'text-rose-400'}`}>
              {health?.components?.gateway || 'CONNECTING...'}
            </span>
          </div>

          <span className="text-slate-700">|</span>

          <div className="flex items-center gap-1.5 text-slate-300">
            <Server className="w-3.5 h-3.5 text-amber-400" />
            <span>Redis:</span>
            <span className={`font-semibold ${health?.components?.redis === 'UP' ? 'text-emerald-400' : 'text-rose-400'}`}>
              {health?.components?.redis || 'PENDING'}
            </span>
          </div>

          <span className="text-slate-700">|</span>

          <div className="flex items-center gap-1.5 text-slate-300">
            <Database className="w-3.5 h-3.5 text-indigo-400" />
            <span>Postgres:</span>
            <span className={`font-semibold ${health?.components?.database === 'UP' ? 'text-emerald-400' : 'text-rose-400'}`}>
              {health?.components?.database || 'PENDING'}
            </span>
          </div>
        </div>

        <button className="p-2 bg-slate-800 hover:bg-slate-700 rounded-lg text-slate-300 transition-colors relative">
          <Bell className="w-4 h-4" />
          <span className="absolute top-1 right-1 w-2 h-2 bg-cyan-400 rounded-full animate-pulse"></span>
        </button>

        <div className="flex items-center gap-2 pl-2 border-l border-slate-800">
          <div className="w-8 h-8 rounded-full bg-gradient-to-tr from-cyan-600 to-indigo-600 flex items-center justify-center font-bold text-xs text-white">
            SA
          </div>
          <div className="text-xs">
            <div className="font-semibold text-slate-200">Security Admin</div>
            <div className="text-slate-400 text-[10px]">ADMIN_ROLE</div>
          </div>
        </div>
      </div>
    </header>
  );
};
