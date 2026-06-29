import React, { useEffect, useState, useMemo } from 'react';
import { getUsers, updateUserStatus } from '../api/userApi';
import Card from '../components/ui/Card';
import Table from '../components/ui/Table';
import Badge from '../components/ui/Badge';
import Button from '../components/ui/Button';
import { useToast } from '../components/ui/Toast';
import { formatEpoch } from '../utils/formatDate';
import EmployeeModal from '../components/employees/EmployeeModal';
import PasswordModal from '../components/employees/PasswordModal';

export default function EmployeePage() {
  const { showToast } = useToast();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Modals state
  const [isEmployeeModalOpen, setIsEmployeeModalOpen] = useState(false);
  const [isPasswordModalOpen, setIsPasswordModalOpen] = useState(false);
  const [selectedEmployee, setSelectedEmployee] = useState(null);

  // Search & Filter state
  const [searchTerm, setSearchTerm] = useState('');
  const [roleFilter, setRoleFilter] = useState('ALL');

  const fetchUsersList = async () => {
    setLoading(true);
    setError('');
    try {
      const res = await getUsers();
      setUsers(res.data);
    } catch (err) {
      console.error(err);
      setError('Lỗi khi tải danh sách nhân viên. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsersList();
  }, []);

  const handleEditClick = (employee) => {
    setSelectedEmployee(employee);
    setIsEmployeeModalOpen(true);
  };

  const handleAddClick = () => {
    setSelectedEmployee(null);
    setIsEmployeeModalOpen(true);
  };

  const handlePasswordClick = (employee) => {
    setSelectedEmployee(employee);
    setIsPasswordModalOpen(true);
  };

  const handleToggleStatus = async (employee) => {
    const nextStatus = !employee.isActive;
    const confirmMsg = nextStatus
      ? `Bạn có chắc chắn muốn kích hoạt lại tài khoản "${employee.username}"?`
      : `Bạn có chắc chắn muốn vô hiệu hóa tài khoản "${employee.username}"?`;

    if (window.confirm(confirmMsg)) {
      try {
        await updateUserStatus(employee.userId, nextStatus);
        showToast(
          `${nextStatus ? 'Kích hoạt' : 'Vô hiệu hóa'} tài khoản "${employee.username}" thành công!`,
          'success'
        );
        fetchUsersList();
      } catch (err) {
        console.error(err);
        showToast(err.response?.data?.message || 'Thao tác thất bại.', 'error');
      }
    }
  };

  // Stats
  const stats = useMemo(() => {
    const total = users.length;
    const active = users.filter((u) => u.isActive).length;
    const inactive = total - active;
    return { total, active, inactive };
  }, [users]);

  // Filtered List
  const filteredUsers = useMemo(() => {
    return users.filter((user) => {
      const matchSearch =
        (user.fullName || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
        (user.username || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
        (user.phone || '').includes(searchTerm);

      const matchRole = roleFilter === 'ALL' || user.role === roleFilter;

      return matchSearch && matchRole;
    });
  }, [users, searchTerm, roleFilter]);

  // Table Columns config
  const columns = [
    {
      header: 'STT',
      render: (row, idx) => idx + 1,
      className: 'w-16',
    },
    {
      header: 'Họ tên',
      render: (row) => (
        <div>
          <div className="font-bold text-gray-800">{row.fullName}</div>
          {row.email && <div className="text-xs text-gray-500">📧 {row.email}</div>}
          {row.phone && <div className="text-xs text-gray-400">📞 {row.phone}</div>}
        </div>
      ),
    },
    {
      header: 'Username',
      accessor: 'username',
    },
    {
      header: 'Vai trò',
      render: (row) => {
        const roles = {
          ADMIN: { label: 'Quản trị viên', color: 'red' },
          MANAGER: { label: 'Quản lý', color: 'blue' },
          STAFF: { label: 'Nhân viên', color: 'green' },
        };
        const r = roles[row.role] || { label: row.role, color: 'gray' };
        return <Badge color={r.color}>{r.label}</Badge>;
      },
    },
    {
      header: 'Trạng thái',
      render: (row) => (
        <Badge color={row.isActive ? 'green' : 'gray'}>
          {row.isActive ? 'Hoạt động' : 'Đã vô hiệu'}
        </Badge>
      ),
    },
    {
      header: 'Ngày tạo',
      render: (row) => formatEpoch(row.createdAt),
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
            title="Sửa thông tin"
          >
            <span>✏️</span> <span>Sửa</span>
          </Button>
          <Button
            variant="accent"
            className="px-2 py-1.5 text-xs flex items-center space-x-1"
            onClick={() => handlePasswordClick(row)}
            title="Đổi mật khẩu"
          >
            <span>🔑</span> <span>Mật khẩu</span>
          </Button>
          <Button
            variant={row.isActive ? 'danger' : 'success'}
            className="px-2 py-1.5 text-xs flex items-center space-x-1"
            onClick={() => handleToggleStatus(row)}
            title={row.isActive ? 'Vô hiệu hóa' : 'Kích hoạt'}
          >
            <span>{row.isActive ? '🚫' : '🔄'}</span>
            <span>{row.isActive ? 'Khóa' : 'Mở'}</span>
          </Button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      {/* Header controls */}
      <div className="flex flex-col sm:flex-row justify-between items-center bg-surface p-4 rounded-2xl shadow-sm border border-gray-100 space-y-4 sm:space-y-0">
        <h1 className="text-xl font-extrabold text-primary">👥 Quản lý nhân viên</h1>

        <div className="flex flex-col sm:flex-row items-stretch sm:items-center space-y-2 sm:space-y-0 sm:space-x-3 w-full sm:w-auto">
          {/* Search input */}
          <input
            type="text"
            placeholder="Tìm theo tên hoặc tài khoản..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="px-4 py-2 border rounded-xl outline-none focus:ring-2 focus:ring-accent text-sm text-gray-800"
          />

          {/* Role select */}
          <select
            value={roleFilter}
            onChange={(e) => setRoleFilter(e.target.value)}
            className="px-4 py-2 border rounded-xl outline-none focus:ring-2 focus:ring-accent text-sm bg-white text-gray-800"
          >
            <option value="ALL">Tất cả vai trò</option>
            <option value="ADMIN">Quản trị viên</option>
            <option value="MANAGER">Quản lý</option>
            <option value="STAFF">Nhân viên</option>
          </select>

          {/* Add user button */}
          <Button onClick={handleAddClick} className="flex items-center justify-center space-x-1.5 py-2">
            <span>➕</span> <span>Thêm nhân viên</span>
          </Button>
        </div>
      </div>

      {/* Summary Stats Row */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Card title="Tổng nhân sự" value={`${stats.total} nhân viên`} color="primary" icon="👥" />
        <Card title="Đang hoạt động" value={`${stats.active} tài khoản`} color="success" icon="✅" />
        <Card title="Không hoạt động" value={`${stats.inactive} tài khoản`} color="danger" icon="🚫" />
      </div>

      {/* Employee List Table */}
      {error ? (
        <div className="flex flex-col justify-center items-center h-full min-h-[300px] bg-surface rounded-2xl border border-red-100 p-8 shadow-sm">
          <span className="text-4xl mb-3">⚠️</span>
          <p className="text-red-600 font-semibold mb-4">{error}</p>
          <button onClick={fetchUsersList} className="px-5 py-2.5 bg-primary hover:bg-primary-light text-white font-bold rounded-xl transition shadow">
            Tải lại
          </button>
        </div>
      ) : (
        <Table
          columns={columns}
          data={filteredUsers}
          loading={loading}
          emptyMessage="Không tìm thấy nhân viên nào phù hợp"
        />
      )}

      {/* Edit/Create Modal */}
      <EmployeeModal
        isOpen={isEmployeeModalOpen}
        onClose={() => setIsEmployeeModalOpen(false)}
        employee={selectedEmployee}
        onSave={fetchUsersList}
      />

      {/* Reset Password Modal */}
      <PasswordModal
        isOpen={isPasswordModalOpen}
        onClose={() => setIsPasswordModalOpen(false)}
        employee={selectedEmployee}
      />
    </div>
  );
}
