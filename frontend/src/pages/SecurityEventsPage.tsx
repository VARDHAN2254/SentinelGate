import React, { useEffect, useState, useCallback } from 'react';
import { ShieldAlert, Search, Filter, RefreshCw, ShieldOff } from 'lucide-react';
import { SecurityEvent } from '../types';
import axios from 'axios';

const SEVERITY_STYLES: Record<string, string> = {
  CRITICAL: 'bg-rose-950 text-rose-400 border-rose-800',
  HIGH: 'bg-orange-950 text-orange-400 border-orange-800',
  MEDIUM: 'bg-amber-950 text-amber-400 border-amber-800',
  LOW: 'bg-slate-800 text-slate-300 border-slate-700',
};

function SeverityBadge({ severity }: { severity: string }) {
  return (
    <span className={`px-2 py-0.5 rounded border font-bold text-[10px] ${SEVERITY_STYLES[severity] ?? SEVERITY_STYLES.LOW}`}>
      {severity}
    </span>
  );
}

function SkeletonRow() {
  return (
    <tr>
      {Array.from({ length: 7 }).map((_, i) => (
        <td key={i} className="p-4">
          <div className="h-3 bg-slate-800 rounded animate-pulse" style={{ width: `${60 + Math.random() * 40}%` }} />
        </td>
      ))}
    </tr>
  );
}

export const SecurityEventsPage: React.FC = () => {
  const [events, setEvents] = useState<SecurityEvent[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedSeverity, setSelectedSeverity] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  const fetchEvents = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams({ page: '0', size: '50' });
      if (selectedSeverity !== 'ALL') params.set('severity', selectedSeverity);

      const res = await axios.get<{ content: SecurityEvent[] }>(
        `/api/v1/analytics/events?${params.toString()}`
      );
      setEvents(res.data.content ?? []);
    } catch (err) {
      setError('Failed to load security events. Ensure the backend is running.');
    } finally {
      setLoading(false);
    }
  }, [selectedSeverity]);

  useEffect(() => {
    fetchEvents();
  }, [fetchEvents]);

  const filteredEvents = events.filter((e) => {
    if (!searchQuery) return true;
    const q = searchQuery.toLowerCase();
    return (
      e.sourceIp.toLowerCase().includes(q) ||
      e.endpoint.toLowerCase().includes(q) ||
      e.eventType.toLowerCase().includes(q) ||
      (e.clientIdentity?.toLowerCase().includes(q) ?? false)
    );
  });

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-slate-900 border border-slate-800 p-4 rounded-xl">
        <div>
          <h2 className="text-xl font-bold text-slate-100 flex items-center gap-2">
            <ShieldAlert className="w-5 h-5 text-rose-400" />
            Security Events
          </h2>
          <p className="text-sm text-slate-400 mt-0.5">
            Threat events recorded by gateway detection rules
          </p>
        </div>
        <button
          onClick={fetchEvents}
          disabled={loading}
          className="flex items-center gap-2 px-3 py-1.5 bg-slate-800 hover:bg-slate-700 disabled:opacity-50 text-slate-300 text-xs font-medium rounded-lg transition-colors border border-slate-700 self-start sm:self-auto"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          Refresh
        </button>
      </div>

      {/* Error banner */}
      {error && (
        <div className="bg-rose-950/50 border border-rose-800 text-rose-300 text-sm px-4 py-3 rounded-xl flex items-center justify-between">
          {error}
          <button onClick={fetchEvents} className="text-rose-400 hover:text-rose-300 text-xs underline ml-4">
            Retry
          </button>
        </div>
      )}

      {/* Filter bar */}
      <div className="flex flex-col md:flex-row items-center justify-between gap-4 bg-slate-900 border border-slate-800 p-3 rounded-xl">
        <div className="relative w-full md:w-80">
          <Search className="w-4 h-4 text-slate-500 absolute left-3 top-2.5" />
          <input
            type="text"
            placeholder="Filter by IP, endpoint, event type…"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full bg-slate-950 border border-slate-800 rounded-lg pl-9 pr-3 py-1.5 text-xs text-slate-200 focus:outline-none focus:border-cyan-500 font-mono placeholder:font-sans"
          />
        </div>
        <div className="flex items-center gap-2 text-xs font-medium w-full md:w-auto overflow-x-auto">
          <span className="text-slate-400 flex items-center gap-1 shrink-0">
            <Filter className="w-3.5 h-3.5" /> Severity:
          </span>
          {['ALL', 'CRITICAL', 'HIGH', 'MEDIUM', 'LOW'].map((sev) => (
            <button
              key={sev}
              onClick={() => setSelectedSeverity(sev)}
              className={`px-3 py-1 rounded-lg text-xs transition-colors shrink-0 ${
                selectedSeverity === sev
                  ? 'bg-cyan-600 text-white font-bold'
                  : 'bg-slate-800 text-slate-400 hover:bg-slate-700'
              }`}
            >
              {sev}
            </button>
          ))}
        </div>
      </div>

      {/* Events table */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs text-slate-300">
            <thead className="bg-slate-950/80 uppercase text-[11px] font-semibold text-slate-400 border-b border-slate-800">
              <tr>
                <th className="p-4">Timestamp</th>
                <th className="p-4">Event Type</th>
                <th className="p-4">Severity</th>
                <th className="p-4">Source IP</th>
                <th className="p-4">Method / Endpoint</th>
                <th className="p-4">Rule</th>
                <th className="p-4">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60">
              {loading && Array.from({ length: 5 }).map((_, i) => <SkeletonRow key={i} />)}

              {!loading && !error && filteredEvents.length === 0 && (
                <tr>
                  <td colSpan={7} className="p-12 text-center">
                    <ShieldOff className="w-8 h-8 mx-auto text-slate-700 mb-3" />
                    <p className="text-slate-500 font-sans">
                      {searchQuery || selectedSeverity !== 'ALL'
                        ? 'No events match the current filter.'
                        : 'No security events recorded yet.'}
                    </p>
                    {!searchQuery && selectedSeverity === 'ALL' && (
                      <p className="text-slate-600 text-[11px] mt-1 font-sans">
                        Events appear here when gateway security rules are triggered.
                      </p>
                    )}
                  </td>
                </tr>
              )}

              {!loading &&
                filteredEvents.map((evt) => (
                  <tr key={evt.id} className="hover:bg-slate-800/30 transition-colors">
                    <td className="p-4 text-slate-400 text-[11px] whitespace-nowrap font-mono">
                      {new Date(evt.timestamp).toLocaleString(undefined, {
                        month: 'short', day: 'numeric',
                        hour: '2-digit', minute: '2-digit', second: '2-digit',
                      })}
                    </td>
                    <td className="p-4 font-bold text-slate-200 whitespace-nowrap">{evt.eventType}</td>
                    <td className="p-4">
                      <SeverityBadge severity={evt.severity} />
                    </td>
                    <td className="p-4 text-cyan-400 font-mono">{evt.sourceIp}</td>
                    <td className="p-4 font-mono text-slate-300 max-w-[220px] truncate">
                      <span className="text-slate-500 mr-1">{evt.httpMethod}</span>
                      {evt.endpoint}
                    </td>
                    <td className="p-4 text-amber-400 text-[11px] whitespace-nowrap">
                      {evt.ruleTriggered ?? '—'}
                    </td>
                    <td className="p-4">
                      <span className="px-2 py-0.5 rounded bg-slate-800 text-slate-300 border border-slate-700 text-[10px]">
                        {evt.actionTaken}
                      </span>
                    </td>
                  </tr>
                ))}
            </tbody>
          </table>
        </div>
      </div>

      {!loading && filteredEvents.length > 0 && (
        <p className="text-xs text-slate-600 text-right">
          Showing {filteredEvents.length} events · page 1 of 1 (max 50 per page)
        </p>
      )}
    </div>
  );
};
