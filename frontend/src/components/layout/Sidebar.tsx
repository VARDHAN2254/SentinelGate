import React from 'react';
import { LayoutDashboard, Route, Key, ShieldAlert, Sliders, ClipboardList, Activity } from 'lucide-react';

interface SidebarProps {
  activeTab: string;
  setActiveTab: (tab: string) => void;
}

export const Sidebar: React.FC<SidebarProps> = ({ activeTab, setActiveTab }) => {
  const navItems = [
    { id: 'dashboard', label: 'Security Dashboard', icon: LayoutDashboard },
    { id: 'routes', label: 'Registered Routes & Services', icon: Route },
    { id: 'apikeys', label: 'API Keys', icon: Key },
    { id: 'security-events', label: 'Security Events', icon: ShieldAlert },
    { id: 'security-rules', label: 'Detection Rules', icon: Sliders },
    { id: 'audit-logs', label: 'Audit Trail', icon: ClipboardList },
    { id: 'observability', label: 'Prometheus & Metrics', icon: Activity },
  ];

  return (
    <aside className="w-64 bg-slate-900 border-r border-slate-800 flex flex-col justify-between shrink-0">
      <div className="p-4 space-y-1">
        <div className="px-3 py-2 text-xs font-semibold text-slate-500 uppercase tracking-wider">
          Operations
        </div>
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive = activeTab === item.id;
          return (
            <button
              key={item.id}
              onClick={() => setActiveTab(item.id)}
              className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-all ${
                isActive
                  ? 'bg-cyan-500/10 text-cyan-400 border border-cyan-500/30'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
              }`}
            >
              <Icon className={`w-4 h-4 ${isActive ? 'text-cyan-400' : 'text-slate-400'}`} />
              <span>{item.label}</span>
            </button>
          );
        })}
      </div>

      <div className="p-4 border-t border-slate-800 bg-slate-950/40">
        <div className="bg-slate-900 border border-slate-800 rounded-lg p-3 text-xs space-y-2">
          <div className="font-semibold text-slate-300">Rate Limiter Mode</div>
          <div className="flex items-center gap-2 text-slate-400">
            <span className="w-2 h-2 rounded-full bg-emerald-400"></span>
            Redis Sliding Window
          </div>
          <div className="text-[11px] text-slate-500 font-mono">Default: 100 req/min/IP</div>
        </div>
      </div>
    </aside>
  );
};
