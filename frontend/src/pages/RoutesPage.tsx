import React, { useEffect, useState } from 'react';
import { Route, Plus, Trash2, RefreshCw, Server } from 'lucide-react';
import axios from 'axios';

interface GatewayRoute {
  id: number;
  routeId: string;
  serviceId: number;
  serviceName: string;
  serviceBaseUrl: string;
  pathPattern: string;
  requiresAuth: boolean;
  allowedRoles: string;
  rateLimitPerMin: number;
  isActive: boolean;
}

export const RoutesPage: React.FC = () => {
  const [routes, setRoutes] = useState<GatewayRoute[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [showModal, setShowModal] = useState<boolean>(false);

  // New Route Form State
  const [routeId, setRouteId] = useState<string>('');
  const [serviceName, setServiceName] = useState<string>('');
  const [baseUrl, setBaseUrl] = useState<string>('');
  const [pathPattern, setPathPattern] = useState<string>('');

  const fetchData = async () => {
    setLoading(true);
    try {
      const routesRes = await axios.get('/api/v1/admin/routes').catch(() => ({ data: [] }));
      setRoutes(routesRes.data || []);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleCreateRoute = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      // 1. Create Service
      const serviceRes = await axios.post('/api/v1/admin/services', {
        name: serviceName,
        baseUrl: baseUrl,
        healthEndpoint: '/actuator/health'
      });

      // 2. Create Route
      await axios.post('/api/v1/admin/routes', {
        routeId: routeId,
        serviceId: serviceRes.data.id,
        pathPattern: pathPattern,
        requiresAuth: true,
        allowedRoles: 'ADMIN,DEVELOPER',
        rateLimitPerMin: 500,
        isActive: true
      });

      setShowModal(false);
      setRouteId('');
      setServiceName('');
      setBaseUrl('');
      setPathPattern('');
      fetchData();
    } catch (err) {
      alert('Error creating route');
    }
  };

  const handleDeleteRoute = async (id: number) => {
    if (confirm('Are you sure you want to delete this gateway route?')) {
      await axios.delete(`/api/v1/admin/routes/${id}`);
      fetchData();
    }
  };

  return (
    <div className="p-6 space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-slate-900 border border-slate-800 p-4 rounded-xl">
        <div>
          <h2 className="text-xl font-bold text-slate-100 flex items-center gap-2">
            <Route className="w-5 h-5 text-cyan-400" />
            Registered Service Routes
          </h2>
          <p className="text-sm text-slate-400">Manage dynamic API gateway reverse-proxy paths and targets</p>
        </div>

        <div className="flex items-center gap-3">
          <button 
            onClick={fetchData}
            className="flex items-center gap-2 px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-medium rounded-lg transition-colors border border-slate-700"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
            Refresh
          </button>
          <button
            onClick={() => setShowModal(true)}
            className="flex items-center gap-2 px-4 py-2 bg-cyan-600 hover:bg-cyan-500 text-white text-xs font-bold rounded-lg transition-all shadow-lg shadow-cyan-600/20"
          >
            <Plus className="w-4 h-4" />
            Register New Route
          </button>
        </div>
      </div>

      {/* Routes Table */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs text-slate-300">
            <thead className="bg-slate-950/80 uppercase text-[11px] font-semibold text-slate-400 border-b border-slate-800">
              <tr>
                <th className="p-4">Route ID</th>
                <th className="p-4">Service Name</th>
                <th className="p-4">Path Pattern</th>
                <th className="p-4">Target Base URL</th>
                <th className="p-4">Rate Limit</th>
                <th className="p-4">Status</th>
                <th className="p-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 font-mono">
              {routes.length === 0 && (
                <tr>
                  <td colSpan={7} className="p-8 text-center text-slate-500 font-sans">
                    No custom routes registered yet.
                  </td>
                </tr>
              )}
              {routes.map((r) => (
                <tr key={r.id} className="hover:bg-slate-800/30 transition-colors">
                  <td className="p-4 font-bold text-cyan-400">{r.routeId}</td>
                  <td className="p-4 font-sans font-semibold text-slate-200">{r.serviceName}</td>
                  <td className="p-4 text-emerald-400">{r.pathPattern}</td>
                  <td className="p-4 text-slate-400">{r.serviceBaseUrl}</td>
                  <td className="p-4 text-slate-300">{r.rateLimitPerMin} req/min</td>
                  <td className="p-4 font-sans">
                    <span className="px-2 py-0.5 rounded bg-emerald-950 text-emerald-400 border border-emerald-800 text-[10px] font-bold">
                      ACTIVE
                    </span>
                  </td>
                  <td className="p-4 text-right">
                    <button 
                      onClick={() => handleDeleteRoute(r.id)}
                      className="p-1.5 hover:bg-rose-950 text-rose-400 rounded transition-colors"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Modal Form */}
      {showModal && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-xl max-w-md w-full p-6 space-y-4 shadow-2xl">
            <h3 className="text-lg font-bold text-slate-100 flex items-center gap-2">
              <Server className="w-5 h-5 text-cyan-400" />
              Register Target Backend Route
            </h3>
            
            <form onSubmit={handleCreateRoute} className="space-y-3 text-xs">
              <div>
                <label className="block text-slate-400 mb-1 font-semibold">Route ID</label>
                <input 
                  type="text" 
                  required
                  placeholder="e.g. payment_service_route"
                  value={routeId}
                  onChange={(e) => setRouteId(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg p-2.5 text-slate-100 focus:outline-none focus:border-cyan-500 font-mono"
                />
              </div>

              <div>
                <label className="block text-slate-400 mb-1 font-semibold">Service Name</label>
                <input 
                  type="text" 
                  required
                  placeholder="e.g. Payment Microservice"
                  value={serviceName}
                  onChange={(e) => setServiceName(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg p-2.5 text-slate-100 focus:outline-none focus:border-cyan-500"
                />
              </div>

              <div>
                <label className="block text-slate-400 mb-1 font-semibold">Path Pattern</label>
                <input 
                  type="text" 
                  required
                  placeholder="e.g. /api/v1/payments/**"
                  value={pathPattern}
                  onChange={(e) => setPathPattern(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg p-2.5 text-slate-100 focus:outline-none focus:border-cyan-500 font-mono"
                />
              </div>

              <div>
                <label className="block text-slate-400 mb-1 font-semibold">Target Base URL</label>
                <input 
                  type="text" 
                  required
                  placeholder="e.g. http://payment-service:8083"
                  value={baseUrl}
                  onChange={(e) => setBaseUrl(e.target.value)}
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
                  Save & Apply
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
