import React, { useState } from 'react';
import { Header } from './components/layout/Header';
import { Sidebar } from './components/layout/Sidebar';
import { Dashboard } from './pages/Dashboard';
import { RoutesPage } from './pages/RoutesPage';
import { ApiKeysPage } from './pages/ApiKeysPage';
import { SecurityEventsPage } from './pages/SecurityEventsPage';
import { SecurityRulesPage } from './pages/SecurityRulesPage';
import { AuditLogsPage } from './pages/AuditLogsPage';

export const App: React.FC = () => {
  const [activeTab, setActiveTab] = useState<string>('dashboard');

  return (
    <div className="min-h-screen bg-slate-950 flex flex-col font-sans">
      <Header />
      <div className="flex-1 flex overflow-hidden">
        <Sidebar activeTab={activeTab} setActiveTab={setActiveTab} />
        <main className="flex-1 overflow-y-auto bg-slate-950">
          {activeTab === 'dashboard' && <Dashboard />}
          {activeTab === 'routes' && <RoutesPage />}
          {activeTab === 'apikeys' && <ApiKeysPage />}
          {activeTab === 'security-events' && <SecurityEventsPage />}
          {activeTab === 'security-rules' && <SecurityRulesPage />}
          {activeTab === 'audit-logs' && <AuditLogsPage />}
          {activeTab === 'observability' && (
            <div className="p-6 space-y-6">
              <div className="bg-slate-900 border border-slate-800 p-5 rounded-xl">
                <h2 className="text-xl font-bold text-slate-100 mb-1">Observability Stack</h2>
                <p className="text-sm text-slate-400">
                  SentinelGate exports Micrometer metrics to Prometheus and pre-provisions a Grafana datasource.
                </p>
              </div>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <a
                  href="http://localhost:3000"
                  target="_blank"
                  rel="noreferrer"
                  className="block bg-slate-900 border border-slate-800 hover:border-cyan-700 p-5 rounded-xl transition-colors group"
                >
                  <div className="text-xs font-bold text-cyan-400 uppercase tracking-wider mb-2">Grafana</div>
                  <div className="text-slate-200 font-semibold group-hover:text-white">Dashboard & Alerting</div>
                  <div className="text-xs text-slate-500 mt-1">localhost:3000 · admin / admin</div>
                  <div className="text-xs text-slate-600 mt-3">
                    Prometheus datasource auto-provisioned. Import a dashboard or build custom panels for{' '}
                    <code className="text-cyan-500">sentinelgate.requests.total</code> and{' '}
                    <code className="text-cyan-500">sentinelgate.ratelimit.violations.total</code>.
                  </div>
                </a>
                <a
                  href="http://localhost:9090"
                  target="_blank"
                  rel="noreferrer"
                  className="block bg-slate-900 border border-slate-800 hover:border-cyan-700 p-5 rounded-xl transition-colors group"
                >
                  <div className="text-xs font-bold text-amber-400 uppercase tracking-wider mb-2">Prometheus</div>
                  <div className="text-slate-200 font-semibold group-hover:text-white">Raw Metrics Explorer</div>
                  <div className="text-xs text-slate-500 mt-1">localhost:9090 · scrapes /actuator/prometheus every 5s</div>
                  <div className="text-xs text-slate-600 mt-3">
                    Scrape target: <code className="text-amber-500">sentinelgate-backend:8080</code>
                  </div>
                </a>
              </div>
            </div>
          )}
        </main>
      </div>
    </div>
  );
};

export default App;
