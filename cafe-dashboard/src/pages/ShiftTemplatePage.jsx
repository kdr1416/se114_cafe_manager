import React, { useEffect, useState, useMemo } from 'react';
import { getAllTemplates, deactivateTemplate } from '../api/shiftTemplateApi';
import Card from '../components/ui/Card';
import Table from '../components/ui/Table';
import Badge from '../components/ui/Badge';
import Button from '../components/ui/Button';
import { useToast } from '../components/ui/Toast';
import { formatEpoch } from '../utils/formatDate';
import ShiftTemplateModal from '../components/shifts/ShiftTemplateModal';

export default function ShiftTemplatePage() {
  const { showToast } = useToast();
  const [templates, setTemplates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Modal State
  const [isOpen, setIsOpen] = useState(false);
  const [selectedTemplate, setSelectedTemplate] = useState(null);

  const fetchTemplates = async () => {
    setLoading(true);
    setError('');
    try {
      const res = await getAllTemplates();
      setTemplates(res.data);
    } catch (err) {
      console.error(err);
      setError('Lỗi khi tải danh sách mẫu ca. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTemplates();
  }, []);

  const handleAddClick = () => {
    setSelectedTemplate(null);
    setIsOpen(true);
  };

  const handleEditClick = (tpl) => {
    setSelectedTemplate(tpl);
    setIsOpen(true);
  };

  const handleDeactivate = async (tpl) => {
    if (window.confirm(`Bạn có chắc chắn muốn ngừng hoạt động mẫu ca "${tpl.templateName}"?`)) {
      try {
        await deactivateTemplate(tpl.templateId);
        showToast(`Đã ngừng hoạt động mẫu ca "${tpl.templateName}" thành công!`, 'success');
        fetchTemplates();
      } catch (err) {
        console.error(err);
        showToast(err.response?.data?.message || 'Không thể cập nhật trạng thái mẫu ca.', 'error');
      }
    }
  };

  // Stats
  const stats = useMemo(() => {
    const total = templates.length;
    const active = templates.filter(t => t.isActive).length;
    const inactive = total - active;
    return { total, active, inactive };
  }, [templates]);

  // Table Columns config
  const columns = [
    {
      header: 'STT',
      render: (row, idx) => idx + 1,
      className: 'w-16',
    },
    {
      header: 'Tên mẫu ca',
      accessor: 'templateName',
      className: 'font-bold text-gray-800',
    },
    {
      header: 'Giờ làm việc',
      render: (row) => `${row.startTime} - ${row.endTime}`,
    },
    {
      header: 'Số nhân sự tối thiểu',
      render: (row) => `${row.minStaff} nhân viên`,
    },
    {
      header: 'Thời gian áp dụng',
      render: (row) => {
        if (row.effectiveFromDate && row.effectiveToDate) {
          return `${formatEpoch(row.effectiveFromDate)} đến ${formatEpoch(row.effectiveToDate)}`;
        }
        if (row.effectiveFromDate) {
          return `Từ ${formatEpoch(row.effectiveFromDate)}`;
        }
        return <span className="text-gray-400 italic">Vĩnh viễn</span>;
      },
    },
    {
      header: 'Trạng thái',
      render: (row) => (
        <Badge color={row.isActive ? 'green' : 'gray'}>
          {row.isActive ? 'Hoạt động' : 'Ngưng hoạt động'}
        </Badge>
      ),
    },
    {
      header: 'Hành động',
      className: 'text-right',
      render: (row) => (
        <div className="flex justify-end space-x-2">
          <Button
            variant="secondary"
            className="px-2 py-1.5 text-xs flex items-center space-x-1"
            onClick={() => handleEditClick(row)}
            title="Sửa mẫu ca"
          >
            <span>✏️</span> <span>Sửa</span>
          </Button>
          {row.isActive && (
            <Button
              variant="warning"
              className="px-2 py-1.5 text-xs flex items-center space-x-1"
              onClick={() => handleDeactivate(row)}
              title="Ngừng hoạt động"
            >
              <span>🚫</span> <span>Khóa</span>
            </Button>
          )}
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      {/* Header controls */}
      <div className="flex flex-col sm:flex-row justify-between items-center bg-surface p-4 rounded-2xl shadow-sm border border-gray-100 space-y-4 sm:space-y-0">
        <h1 className="text-xl font-extrabold text-primary">📅 Quản lý mẫu ca trực</h1>
        <Button onClick={handleAddClick} className="flex items-center justify-center space-x-1.5 py-2">
          <span>➕</span> <span>Thêm mẫu ca</span>
        </Button>
      </div>

      {/* Stats Cards Row */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Card title="Tổng số mẫu ca" value={`${stats.total} mẫu`} color="primary" icon="📅" />
        <Card title="Đang áp dụng" value={`${stats.active} ca trực`} color="success" icon="✅" />
        <Card title="Ngưng hoạt động" value={`${stats.inactive} ca trực`} color="danger" icon="🚫" />
      </div>

      {/* Templates List Table */}
      {error ? (
        <div className="flex flex-col justify-center items-center h-full min-h-[300px] bg-surface rounded-2xl border border-red-100 p-8 shadow-sm">
          <span className="text-4xl mb-3">⚠️</span>
          <p className="text-red-600 font-semibold mb-4">{error}</p>
          <button onClick={fetchTemplates} className="px-5 py-2.5 bg-primary hover:bg-primary-light text-white font-bold rounded-xl transition shadow">
            Tải lại
          </button>
        </div>
      ) : (
        <Table
          columns={columns}
          data={templates}
          loading={loading}
          emptyMessage="Chưa có mẫu ca trực nào được tạo trong hệ thống"
        />
      )}

      {/* Create/Edit Modal */}
      <ShiftTemplateModal
        isOpen={isOpen}
        onClose={() => setIsOpen(false)}
        template={selectedTemplate}
        onSave={fetchTemplates}
      />
    </div>
  );
}
