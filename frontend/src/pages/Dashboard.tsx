import React, { useEffect, useState, useCallback } from 'react';
import {
  ShieldCheck,
  ShieldAlert,
  AlertTriangle,
  Activity,
  Lock,
  RefreshCw,
  Server,
  Key,
} from 'lucide-react';
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';
import {
  SecurityMetricsOverview,
  TrafficBucket,
  GatewayRoute,
  SecurityRule,
} from '../types';
import axios from 'axios';

const EMPTY_METRICS: SecurityMetricsOverview = {
  totalSecurityEvents: 0,
  blockedRequests: 0,
  authFailures: 0,
  bruteForceEvents: 0,
  suspiciousIps: 0,
  rateLimitHits: 0,
  recentEventsLastHour: 0,
  requestsPerSecond: 0,
  activeRoutes: 0,
  activeApiKeys: 0,
};

function StatusDot({ status }: { status: string }) {
  const isHealthy = status === 'HEALTHY';
  return (
    <span
      className={`px-2 py-0.5 rounded-full text-[10px] font-bold font-mono border ${
        isHealthy
          ? 'bg-emerald-950 text-emerald-400 border-emerald-800'
          : 'bg-amber-950 text-amber-400 border-amber-800'
      }`}
    >
      {status}
    </span>
  );
}

function MetricCard({
  label,
  value,
  sub,
  icon: Icon,
  color,
}: {
  label: string;
  value: React.ReactNode;
  sub: string;
  icon: React.ElementType;
  color: string;
}) {
  return (
    <div className="bg-slate-900 border border-slate-800 p-4 rounded-xl space-y-2">
      <div className="flex items-center justify-between text-slate-400">
        <span className="text-xs font-semibold uppercase tracking-wider">{label}</span>
        <Icon className={`w-4 h-4 ${color}`} />
      </div>
      <div className="text-2xl font-bold font-mono text-slate-100">{value}</div>
      <p className="text-xs text-slate-500">{sub}</p>
    </div>
  );
}

export const Dashboard: React.FC = () => {
  const [metrics, setMetrics] = useState<SecurityMetricsOverview>(EMPTY_METRICS);
  const [timeline, setTimeline] = useState<TrafficBucket[]>([]);
  const [routes, setRoutes] = useState<GatewayRoute[]>([]);
  const [rules, setRules] = useState<SecurityRule[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchAll = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [metricsRes, timelineRes, routesRes, rulesRes] = await Promise.allSettled([
        axios.get<SecurityMetricsOverview>('/api/v1/analytics/overview'),
        axios.get<TrafficBucket[]>('/api/v1/analytics/traffic-timeline'),
        axios.get<{ content?: GatewayRoute[] } | GatewayRoute[]>('/api/v1/admin/routes'),
        axios.get<SecurityRule[]>('/api/v1/admin/security-rules'),
      ]);

      if (metricsRes.status === 'fulfilled') setMetrics(metricsRes.value.data);
      if (timelineRes.status === 'fulfilled') setTimeline(timelineRes.value.data);
      if (routesRes.status === 'fulfilled') {
        const data = routesRes.value.data;
        setRoutes(Array.isArray(data) ? data : (data as any).content ?? []);
      }
      if (rulesRes.status === 'fulfilled') {
        const data = rulesRes.value.data;
        setRules(Array.isArray(data) ? data : []);
      }
    } catch {
      setError('Failed to fetch dashboard data. Is the backend running?');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchAll();
  }, [fetchAll]);

  const blockRate =
    metrics.totalSecurityEvents > 0
      ? ((metrics.blockedRequests / metrics.totalSecurityEvents) * 100).toFixed(1)
      : '0.0';

  return (
    <div className="p-6 space-y-6">
      {/* Header bar */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-slate-900 border border-slate-800 p-4 rounded-xl">
        <div>
          <h2 className="text-xl font-bold text-slate-100 flex items-center gap-2">
            Security Overview
            <span className="text-xs px-2 py-0.5 rounded bg-emerald-950 text-emerald-400 border border-emerald-800 flex items-center gap-1 font-normal">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
              Gateway Active
            </span>
          </h2>
          <p className="text-sm text-slate-400 mt-0.5">
            Live telemetry from SentinelGate security event log
          </p>
        </div>
        <button
          onClick={fetchAll}
          disabled={loading}
          className="flex items-center gap-2 px-3 py-1.5 bg-slate-800 hover:bg-slate-700 disabled:opacity-50 text-slate-300 text-xs font-medium rounded-lg transition-colors border border-slate-700"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          Refresh
        </button>
      </div>

      {/* Error banner */}
      {error && (
        <div className="bg-rose-950/50 border border-rose-800 text-rose-300 text-sm px-4 py-3 rounded-xl">
          {error}
        </div>
      )}

      {/* KPI cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <MetricCard
          label="Security Events"
          value={metrics.totalSecurityEvents.toLocaleString()}
          sub={`${metrics.recentEventsLastHour} in the last hour`}
          icon={Activity}
          color="text-cyan-400"
        />
        <MetricCard
          label="Blocked Traffic"
          value={
            <span className="text-rose-400">
              {metrics.blockedRequests.toLocaleString()}
            </span>
          }
          sub={`${blockRate}% of total events blocked`}
          icon={ShieldAlert}
          color="text-rose-400"
        />
        <MetricCard
          label="Auth Failures"
          value={
            <span className="text-amber-400">{metrics.authFailures}</span>
          }
          sub={`${metrics.suspiciousIps} suspicious IPs tracked`}
          icon={Lock}
          color="text-amber-400"
        />
        <MetricCard
          label="Rate Limit Hits"
          value={
            <span className="text-indigo-400">{metrics.rateLimitHits}</span>
          }
          sub={`${metrics.requestsPerSecond} events/sec (last hour avg)`}
          icon={AlertTriangle}
          color="text-indigo-400"
        />
      </div>

      {/* Traffic timeline chart */}
      <div className="bg-slate-900 border border-slate-800 p-5 rounded-xl space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-base font-bold text-slate-200">
              Security Event Timeline
            </h3>
            <p className="text-xs text-slate-400">
              Events recorded per 5-minute window (last hour) — source: security_events table
            </p>
          </div>
          <div className="flex items-center gap-4 text-xs font-medium">
            <span className="flex items-center gap-1.5 text-cyan-400">
              <span className="w-2.5 h-2.5 rounded-full bg-cyan-500" /> Total Events
            </span>
            <span className="flex items-center gap-1.5 text-rose-400">
              <span className="w-2.5 h-2.5 rounded-full bg-rose-500" /> Blocked
            </span>
          </div>
        </div>

        <div className="h-64 w-full">
          {timeline.length === 0 && !loading ? (
            <div className="h-full flex items-center justify-center text-slate-500 text-sm">
              <div className="text-center space-y-2">
                <ShieldCheck className="w-8 h-8 mx-auto text-slate-700" />
                <p>No events recorded in the last hour.</p>
                <p className="text-xs text-slate-600">
                  Events appear here when security rules are triggered.
                </p>
              </div>
            </div>
          ) : (
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart
                data={timeline}
                margin={{ top: 10, right: 10, left: -20, bottom: 0 }}
              >
                <defs>
                  <linearGradient id="colorTotal" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#06b6d4" stopOpacity={0.4} />
                    <stop offset="95%" stopColor="#06b6d4" stopOpacity={0} />
                  </linearGradient>
                  <linearGradient id="colorBlocked" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#ef4444" stopOpacity={0.5} />
                    <stop offset="95%" stopColor="#ef4444" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                <XAxis dataKey="windowStart" stroke="#64748b" fontSize={11} />
                <YAxis stroke="#64748b" fontSize={11} allowDecimals={false} />
                <Tooltip
                  contentStyle={{
                    backgroundColor: '#0f172a',
                    borderColor: '#334155',
                    borderRadius: '0.5rem',
                    color: '#f8fafc',
                    fontSize: '12px',
                  }}
                />
                <Area
                  type="monotone"
                  dataKey="totalEvents"
                  name="Total Events"
                  stroke="#06b6d4"
                  fillOpacity={1}
                  fill="url(#colorTotal)"
                />
                <Area
                  type="monotone"
                  dataKey="blockedEvents"
                  name="Blocked"
                  stroke="#ef4444"
                  fillOpacity={1}
                  fill="url(#colorBlocked)"
                />
              </AreaChart>
            </ResponsiveContainer>
          )}
        </div>
      </div>

      {/* Routes + Security Rules */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Registered routes */}
        <div className="bg-slate-900 border border-slate-800 p-5 rounded-xl space-y-4">
          <h3 className="text-base font-bold text-slate-200 flex items-center justify-between">
            <span className="flex items-center gap-2">
              <Server className="w-4 h-4 text-cyan-400" />
              Registered Routes
            </span>
            <span className="text-xs text-slate-400 font-mono font-normal">
              {metrics.activeRoutes} active
            </span>
          </h3>
          {loading ? (
            <div className="space-y-2">
              {[1, 2].map((i) => (
                <div key={i} className="h-14 bg-slate-800/50 rounded-lg animate-pulse" />
              ))}
            </div>
          ) : routes.length === 0 ? (
            <p className="text-sm text-slate-500">No routes registered. Add a backend service to get started.</p>
          ) : (
            <div className="space-y-3">
              {routes.map((route) => (
                <div
                  key={route.id}
                  className="flex items-center justify-between p-3 bg-slate-950/60 border border-slate-800 rounded-lg text-xs"
                >
                  <div className="space-y-1 min-w-0">
                    <div className="font-bold text-slate-200 flex items-center gap-2 truncate">
                      {route.service?.name ?? 'Unknown Service'}
                      <span className="px-1.5 py-0.5 rounded bg-slate-800 text-cyan-400 font-mono text-[10px]">
                        {route.pathPattern}
                      </span>
                    </div>
                    <div className="text-slate-400 font-mono truncate">
                      {route.service?.baseUrl ?? '—'}
                    </div>
                  </div>
                  <StatusDot status={route.service?.status ?? 'UNKNOWN'} />
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Active security rules */}
        <div className="bg-slate-900 border border-slate-800 p-5 rounded-xl space-y-4">
          <h3 className="text-base font-bold text-slate-200 flex items-center justify-between">
            <span className="flex items-center gap-2">
              <Key className="w-4 h-4 text-cyan-400" />
              Security Rules
            </span>
            <span className="text-xs text-slate-400 font-mono font-normal">
              {metrics.activeApiKeys} active API keys
            </span>
          </h3>
          {loading ? (
            <div className="space-y-2">
              {[1, 2, 3].map((i) => (
                <div key={i} className="h-14 bg-slate-800/50 rounded-lg animate-pulse" />
              ))}
            </div>
          ) : rules.length === 0 ? (
            <p className="text-sm text-slate-500">No security rules configured.</p>
          ) : (
            <div className="space-y-3 text-xs">
              {rules.map((rule) => (
                <div
                  key={rule.id}
                  className="p-3 bg-slate-950/60 border border-slate-800 rounded-lg flex items-center justify-between"
                >
                  <div>
                    <div className="font-semibold text-slate-200">{rule.name}</div>
                    {rule.description && (
                      <div className="text-slate-400 mt-0.5">{rule.description}</div>
                    )}
                  </div>
                  <span
                    className={`font-mono px-2 py-1 rounded border text-[10px] font-bold ${
                      rule.isEnabled
                        ? 'text-cyan-400 bg-cyan-950 border-cyan-800'
                        : 'text-slate-500 bg-slate-800 border-slate-700'
                    }`}
                  >
                    {rule.isEnabled ? 'ACTIVE' : 'DISABLED'}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
