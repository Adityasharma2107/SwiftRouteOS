import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { Package, AlertTriangle, CheckCircle, RefreshCw, Lock } from 'lucide-react';
import { inventoryApi } from '../../api/inventory';
import type { InventoryItem } from '../../types';

export const InventoryView: React.FC = () => {
  const {
    data: items = [],
    isLoading,
    refetch,
  } = useQuery({
    queryKey: ['inventory-items'],
    queryFn: inventoryApi.getAllItems,
  });

  const lowStockCount = items.filter((i: InventoryItem) => i.isLowStock).length;
  const totalReserved = items.reduce(
    (acc: number, item: InventoryItem) => acc + item.quantityReserved,
    0
  );

  return (
    <div className="space-y-6">
      {/* Header & Metrics */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 shadow-sm flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-base font-bold text-slate-100 flex items-center gap-2">
            <Package className="w-5 h-5 text-blue-400" />
            Inventory & Spare Parts Management
          </h2>
          <p className="text-xs text-slate-400 mt-0.5">
            PostgreSQL row-level pessimistic locking (<code>SELECT ... FOR UPDATE</code>) guarantees zero overselling.
          </p>
        </div>
        <button
          onClick={() => refetch()}
          className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded-lg text-xs font-semibold flex items-center gap-1.5 transition-colors cursor-pointer"
        >
          <RefreshCw className="w-3.5 h-3.5" />
          Refresh Stock
        </button>
      </div>

      {/* Summary KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-4">
          <div className="text-xs font-semibold text-slate-400">Total Catalog Items</div>
          <div className="text-2xl font-black text-slate-100 mt-1">{items.length}</div>
          <div className="text-[11px] text-slate-500 mt-1">Active HVAC and electrical components</div>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-xl p-4">
          <div className="text-xs font-semibold text-sky-400 flex items-center justify-between">
            <span>Locked & Reserved</span>
            <Lock className="w-3.5 h-3.5 text-sky-400" />
          </div>
          <div className="text-2xl font-black text-sky-300 mt-1">{totalReserved} units</div>
          <div className="text-[11px] text-slate-500 mt-1">Committed to active job dispatches</div>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-xl p-4">
          <div className="text-xs font-semibold text-amber-400 flex items-center justify-between">
            <span>Low Stock Alerts</span>
            <AlertTriangle className="w-3.5 h-3.5 text-amber-400" />
          </div>
          <div className="text-2xl font-black text-amber-300 mt-1">{lowStockCount} items</div>
          <div className="text-[11px] text-slate-500 mt-1">Available &le; reorder threshold</div>
        </div>
      </div>

      {/* Items Table */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl overflow-hidden shadow-sm">
        <div className="p-4 border-b border-slate-800 flex items-center justify-between">
          <div className="text-sm font-bold text-slate-200">Catalog Parts Roster</div>
          <div className="text-xs text-slate-400">
            Concurrently guarded with <code>PessimisticWriteLock</code>
          </div>
        </div>

        {isLoading ? (
          <div className="p-12 text-center text-xs text-slate-400 flex flex-col items-center justify-center gap-2">
            <RefreshCw className="w-5 h-5 animate-spin text-blue-500" />
            Loading inventory stock levels...
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-slate-300">
              <thead className="bg-slate-950/60 text-[11px] font-semibold text-slate-400 uppercase tracking-wider border-b border-slate-800">
                <tr>
                  <th className="px-4 py-3">Part Number</th>
                  <th className="px-4 py-3">Part Description</th>
                  <th className="px-4 py-3 text-center">On Hand</th>
                  <th className="px-4 py-3 text-center">Reserved</th>
                  <th className="px-4 py-3 text-center">Available</th>
                  <th className="px-4 py-3 text-center">Threshold</th>
                  <th className="px-4 py-3 text-right">Unit Cost</th>
                  <th className="px-4 py-3 text-right">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {items.map((item) => (
                  <tr key={item.id} className="hover:bg-slate-800/40 transition-colors">
                    <td className="px-4 py-3 font-mono font-bold text-cyan-400">
                      {item.partNumber}
                    </td>
                    <td className="px-4 py-3">
                      <div className="font-semibold text-slate-200">{item.name}</div>
                      <div className="text-[11px] text-slate-400">{item.description}</div>
                    </td>
                    <td className="px-4 py-3 text-center font-mono">{item.quantityOnHand}</td>
                    <td className="px-4 py-3 text-center font-mono text-sky-400 font-semibold">
                      {item.quantityReserved}
                    </td>
                    <td className="px-4 py-3 text-center font-mono font-bold text-emerald-400">
                      {item.quantityAvailable}
                    </td>
                    <td className="px-4 py-3 text-center font-mono text-slate-400">
                      {item.reorderThreshold}
                    </td>
                    <td className="px-4 py-3 text-right font-mono text-slate-300">
                      ${item.unitCost?.toFixed(2) ?? '0.00'}
                    </td>
                    <td className="px-4 py-3 text-right">
                      {item.isLowStock ? (
                        <span className="inline-flex items-center gap-1 text-[11px] font-bold text-amber-400 bg-amber-500/15 border border-amber-500/30 px-2 py-0.5 rounded">
                          <AlertTriangle className="w-3 h-3" />
                          Low Stock
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 text-[11px] font-medium text-emerald-400 bg-emerald-500/10 border border-emerald-500/20 px-2 py-0.5 rounded">
                          <CheckCircle className="w-3 h-3" />
                          Optimal
                        </span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};
