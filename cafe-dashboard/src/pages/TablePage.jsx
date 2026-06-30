import React, { useEffect, useState, useMemo } from 'react';
import { getAreas, deleteArea, getTables, deleteTable } from '../api/tableApi';
import Button from '../components/ui/Button';
import Badge from '../components/ui/Badge';
import Spinner from '../components/ui/Spinner';
import { useToast } from '../components/ui/Toast';
import AreaModal from '../components/tables/AreaModal';
import TableModal from '../components/tables/TableModal';

export default function TablePage() {
  const { showToast } = useToast();

  // Data State
  const [areas, setAreas] = useState([]);
  const [tables, setTables] = useState([]);
  const [selectedAreaName, setSelectedAreaName] = useState('ALL');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Modals state
  const [isAreaOpen, setIsAreaOpen] = useState(false);
  const [selectedArea, setSelectedArea] = useState(null);

  const [isTableOpen, setIsTableOpen] = useState(false);
  const [selectedTable, setSelectedTable] = useState(null);

  const fetchTableData = async () => {
    setLoading(true);
    setError('');
    try {
      const [areasRes, tablesRes] = await Promise.all([
        getAreas(),
        getTables(),
      ]);
      setAreas(areasRes.data);
      setTables(tablesRes.data);
    } catch (err) {
      console.error(err);
      setError('Lỗi khi tải dữ liệu bàn và khu vực. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTableData();
  }, []);

  // Area Actions
  const handleAddArea = () => {
    setSelectedArea(null);
    setIsAreaOpen(true);
  };

  const handleEditArea = (e, area) => {
    e.stopPropagation();
    setSelectedArea(area);
    setIsAreaOpen(true);
  };

  const handleDeleteArea = async (e, area) => {
    e.stopPropagation();
    if (window.confirm(`Bạn có chắc chắn muốn xóa khu vực "${area.areaName}"?\nTất cả bàn thuộc khu vực này cũng sẽ bị ảnh hưởng.`)) {
      try {
        await deleteArea(area.areaId);
        showToast('Xóa khu vực thành công!', 'success');
        if (selectedAreaName === area.areaName) setSelectedAreaName('ALL');
        fetchTableData();
      } catch (err) {
        console.error(err);
        showToast(err.response?.data?.message || 'Không thể xóa khu vực.', 'error');
      }
    }
  };

  // Table Actions
  const handleAddTable = () => {
    setSelectedTable(null);
    setIsTableOpen(true);
  };

  const handleEditTable = (table) => {
    setSelectedTable(table);
    setIsTableOpen(true);
  };

  const handleDeleteTable = async (table) => {
    if (window.confirm(`Bạn có chắc chắn muốn xóa bàn "${table.tableName}"?`)) {
      try {
        await deleteTable(table.tableId);
        showToast('Xóa bàn thành công!', 'success');
        fetchTableData();
      } catch (err) {
        console.error(err);
        showToast('Không thể xóa bàn.', 'error');
      }
    }
  };

  // Filter tables by selected area name
  const filteredTables = useMemo(() => {
    if (selectedAreaName === 'ALL') return tables;
    return tables.filter((t) => t.area === selectedAreaName);
  }, [tables, selectedAreaName]);

  // Compute table count client-side to ensure real-time accuracy
  const computedTableCounts = useMemo(() => {
    const counts = {};
    tables.forEach((t) => {
      counts[t.area] = (counts[t.area] || 0) + 1;
    });
    return counts;
  }, [tables]);

  const getStatusBadge = (status) => {
    const badges = {
      AVAILABLE: { label: 'Trống', color: 'green' },
      OCCUPIED: { label: 'Đang dùng', color: 'red' },
      RESERVED: { label: 'Đặt trước', color: 'yellow' },
    };
    const b = badges[status] || { label: status, color: 'gray' };
    return <Badge color={b.color}>{b.label}</Badge>;
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center h-full min-h-[500px]">
        <Spinner />
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex flex-col justify-center items-center h-full min-h-[400px] bg-surface rounded-2xl border border-red-100 p-8 shadow-sm">
        <span className="text-4xl mb-3">⚠️</span>
        <p className="text-red-600 font-semibold mb-4">{error}</p>
        <button onClick={fetchTableData} className="px-5 py-2.5 bg-primary hover:bg-primary-light text-white font-bold rounded-xl transition shadow">
          Thử lại
        </button>
      </div>
    );
  }

  return (
    <div className="flex flex-col md:flex-row space-y-6 md:space-y-0 md:space-x-6 h-[calc(100vh-100px)] overflow-hidden">
      {/* Left panel: Area List (240px / w-60) */}
      <div className="w-full md:w-60 bg-surface border border-gray-100 rounded-2xl flex flex-col h-full overflow-hidden shadow-sm flex-shrink-0">
        <div className="p-4 border-b border-gray-50 flex items-center justify-between">
          <span className="font-extrabold text-primary">🪑 Khu vực</span>
          <button
            onClick={handleAddArea}
            className="w-8 h-8 rounded-full bg-primary bg-opacity-10 text-primary flex items-center justify-center font-bold hover:bg-opacity-20 transition"
            title="Thêm khu vực"
          >
            ＋
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-3 space-y-1">
          {/* Option: All Areas */}
          <button
            onClick={() => setSelectedAreaName('ALL')}
            className={`w-full text-left px-4 py-2.5 rounded-xl text-sm font-semibold transition flex items-center justify-between ${
              selectedAreaName === 'ALL'
                ? 'bg-primary text-white shadow-sm'
                : 'text-gray-600 hover:bg-gray-50'
            }`}
          >
            <span>Tất cả khu vực</span>
            <span className={`text-xs px-2 py-0.5 rounded-full ${
              selectedAreaName === 'ALL' ? 'bg-white bg-opacity-30 text-white' : 'bg-gray-100 text-gray-500'
            }`}>
              {tables.length}
            </span>
          </button>

          {/* Area List */}
          {areas.map((area) => (
            <button
              key={area.areaId}
              onClick={() => setSelectedAreaName(area.areaName)}
              className={`w-full text-left px-4 py-2.5 rounded-xl text-sm font-semibold transition group flex items-center justify-between ${
                selectedAreaName === area.areaName
                  ? 'bg-primary text-white shadow-sm'
                  : 'text-gray-600 hover:bg-gray-50'
              }`}
            >
              <span className="truncate pr-2">{area.areaName}</span>
              <div className="flex items-center space-x-2 flex-shrink-0">
                <span className={`text-xs px-2 py-0.5 rounded-full ${
                  selectedAreaName === area.areaName ? 'bg-white bg-opacity-30 text-white' : 'bg-gray-100 text-gray-500'
                }`}>
                  {computedTableCounts[area.areaName] || 0}
                </span>

                {/* Edit/Delete Icons (Visible on Hover in Desktop, or when active) */}
                <div className="hidden group-hover:flex items-center space-x-1.5 pl-1">
                  <span
                    onClick={(e) => handleEditArea(e, area)}
                    className="text-xs hover:scale-125 transition duration-150 filter brightness-125 cursor-pointer"
                    title="Sửa"
                  >
                    ✏️
                  </span>
                  <span
                    onClick={(e) => handleDeleteArea(e, area)}
                    className="text-xs hover:scale-125 transition duration-150 filter brightness-125 cursor-pointer"
                    title="Xóa"
                  >
                    🗑️
                  </span>
                </div>
              </div>
            </button>
          ))}
        </div>
      </div>

      {/* Right panel: Tables Grid */}
      <div className="flex-1 bg-surface border border-gray-100 rounded-2xl flex flex-col h-full overflow-hidden shadow-sm">
        {/* Header toolbar */}
        <div className="p-4 border-b border-gray-50 flex justify-between items-center">
          <span className="font-extrabold text-gray-800">
            {selectedAreaName === 'ALL' ? 'Tất cả các bàn' : `Danh sách bàn thuộc: ${selectedAreaName}`}
          </span>
          <Button onClick={handleAddTable} className="flex items-center justify-center space-x-1.5 py-2">
            <span>➕</span> <span>Thêm bàn</span>
          </Button>
        </div>

        {/* Tables Grid */}
        <div className="flex-1 overflow-y-auto p-6 bg-background bg-opacity-30">
          {filteredTables.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-full text-gray-400 py-12">
              <span className="text-4xl block mb-2">🪑</span>
              <p className="font-semibold text-sm">Chưa có bàn nào ở khu vực này</p>
            </div>
          ) : (
            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-6">
              {filteredTables.map((table) => {
                const isAvailable = table.status === 'AVAILABLE';
                return (
                  <div
                    key={table.tableId}
                    className="bg-white rounded-2xl border border-gray-100 shadow-sm p-5 flex flex-col justify-between items-center transition hover:shadow-md text-center space-y-4"
                  >
                    {/* Table Icon & Label */}
                    <div>
                      <div className="text-3xl mb-1">🪑</div>
                      <h4 className="text-base font-extrabold text-gray-800">{table.tableName}</h4>
                      <p className="text-xs text-gray-400 mt-0.5">Khu vực: {table.area}</p>
                    </div>

                    {/* Status & Capacity */}
                    <div className="space-y-1.5">
                      <div>{getStatusBadge(table.status)}</div>
                      <div className="text-xs text-gray-400 font-semibold">Sức chứa: {table.capacity} người</div>
                    </div>

                    {/* Action Row */}
                    <div className="flex items-center justify-center space-x-3 w-full pt-3 border-t border-gray-55">
                      {isAvailable ? (
                        <>
                          <button
                            onClick={() => handleEditTable(table)}
                            className="text-xs text-blue-600 font-bold hover:text-blue-800"
                          >
                            Sửa
                          </button>
                          <span className="text-gray-200">|</span>
                          <button
                            onClick={() => handleDeleteTable(table)}
                            className="text-xs text-red-500 font-bold hover:text-red-700"
                          >
                            Xóa
                          </button>
                        </>
                      ) : (
                        <span className="text-xs text-gray-400 font-medium italic">Không được chỉnh sửa</span>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>

      {/* Area Modal */}
      <AreaModal
        isOpen={isAreaOpen}
        onClose={() => setIsAreaOpen(false)}
        area={selectedArea}
        onSave={fetchTableData}
      />

      {/* Table Modal */}
      <TableModal
        isOpen={isTableOpen}
        onClose={() => setIsTableOpen(false)}
        table={selectedTable}
        areas={areas}
        onSave={fetchTableData}
      />
    </div>
  );
}
