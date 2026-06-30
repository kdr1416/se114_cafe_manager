import React, { useEffect, useState, useMemo } from 'react';
import { getTeamAttendanceReport, getUserAttendanceDetails } from '../api/attendanceApi';
import { getUsers } from '../api/userApi';
import Card from '../components/ui/Card';
import Table from '../components/ui/Table';
import Badge from '../components/ui/Badge';
import Button from '../components/ui/Button';
import Spinner from '../components/ui/Spinner';
import Modal from '../components/ui/Modal';
import { useToast } from '../components/ui/Toast';
import { formatEpoch, formatEpochTime } from '../utils/formatDate';

export default function AttendancePage() {
  const { showToast } = useToast();
  const tzDate = useMemo(() => new Date(new Date().toLocaleString('en-US', { timeZone: 'Asia/Ho_Chi_Minh' })), []);
  const nowYear = tzDate.getFullYear();
  const nowMonth = tzDate.getMonth() + 1;

  const [year, setYear] = useState(nowYear);
  const [month, setMonth] = useState(nowMonth);
  const [employeeId, setEmployeeId] = useState('ALL');
  
  const [employees, setEmployees] = useState([]);
  const [attendanceReport, setAttendanceReport] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Detail Modal state
  const [isDetailOpen, setIsDetailOpen] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailUser, setDetailUser] = useState(null);
  const [detailRecords, setDetailRecords] = useState([]);

  const fetchFiltersAndData = async () => {
    setLoading(true);
    setError('');
    try {
      const [empRes, reportRes] = await Promise.all([
        getUsers(),
        getTeamAttendanceReport(year, month),
      ]);
      setEmployees(empRes.data.filter(u => u.isActive));
      setAttendanceReport(reportRes.data);
    } catch (err) {
      console.error(err);
      setError('Lỗi khi tải báo cáo chấm công. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchFiltersAndData();
  }, [year, month]);

  // Handle detailed record view
  const handleViewDetails = async (row) => {
    setDetailUser(row);
    setIsDetailOpen(true);
    setDetailLoading(true);
    try {
      const res = await getUserAttendanceDetails(row.userId, year, month);
      setDetailRecords(res.data.records || []);
    } catch (err) {
      console.error(err);
      showToast('Không thể tải chi tiết chấm công của nhân viên.', 'error');
      setIsDetailOpen(false);
    } finally {
      setDetailLoading(false);
    }
  };

  // Stats
  const summaryStats = useMemo(() => {
    const totalStaff = attendanceReport.length;
    const totalShifts = attendanceReport.reduce((sum, r) => sum + (r.totalShifts || 0), 0);
    const attendedShifts = attendanceReport.reduce((sum, r) => sum + (r.attendedShifts || 0), 0);
    const absentShifts = attendanceReport.reduce((sum, r) => sum + (r.absentShifts || 0), 0);
    const lateCount = attendanceReport.reduce((sum, r) => sum + (r.lateCount || 0), 0);

    return { totalStaff, totalShifts, attendedShifts, absentShifts, lateCount };
  }, [attendanceReport]);

  // Filter report list by selected employee
  const filteredReport = useMemo(() => {
    if (employeeId === 'ALL') return attendanceReport;
    return attendanceReport.filter(r => r.userId === parseInt(employeeId, 10));
  }, [attendanceReport, employeeId]);

  // CSV Export client-side
  const handleExportCSV = async () => {
    showToast('Đang tạo báo cáo CSV...', 'warning');
    try {
      const csvRows = [];
      // CSV Headers
      csvRows.push('\uFEFFNhân viên,Vai trò,Tổng ca,Có mặt,Vắng,Trễ,Tổng giờ làm,Tỉ lệ đúng giờ (%)');

      filteredReport.forEach(r => {
        csvRows.push(`"${r.fullName}","${r.role}",${r.totalShifts},${r.attendedShifts},${r.absentShifts},${r.lateCount},${r.totalHoursWorked || 0},${r.attendanceRate || 0}`);
      });

      const csvContent = csvRows.join('\n');
      const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `bao_cao_cham_cong_thang_${month}_${year}.csv`);
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      showToast('Xuất báo cáo CSV thành công!', 'success');
    } catch (err) {
      console.error(err);
      showToast('Có lỗi xảy ra khi xuất báo cáo.', 'error');
    }
  };

  const getAttendanceRateColor = (rate) => {
    if (rate >= 80) return 'green';
    if (rate >= 50) return 'yellow';
    return 'red';
  };

  const getProgressColor = (rate) => {
    if (rate >= 80) return 'bg-success';
    if (rate >= 50) return 'bg-warning';
    return 'bg-danger';
  };

  const getStatusBadge = (status) => {
    const badges = {
      COMPLETED: { label: 'Đúng giờ', color: 'green' },
      ON_TIME: { label: 'Đúng giờ', color: 'green' },
      LATE: { label: 'Trễ', color: 'orange' },
      ABSENT: { label: 'Vắng', color: 'red' },
      EARLY_LEAVE: { label: 'Về sớm', color: 'orange' },
      IN_PROGRESS: { label: 'Đang làm', color: 'blue' },
    };
    const b = badges[status] || { label: status, color: 'gray' };
    return <Badge color={b.color}>{b.label}</Badge>;
  };

  // Main columns definition
  const columns = [
    {
      header: 'Nhân viên',
      render: (row) => (
        <div>
          <div className="font-bold text-gray-800">{row.fullName}</div>
          <div className="text-xs text-gray-400">@{row.username} • {row.role}</div>
        </div>
      ),
    },
    {
      header: 'Tổng ca',
      accessor: 'totalShifts',
    },
    {
      header: 'Có mặt',
      accessor: 'attendedShifts',
      className: 'text-success font-bold',
    },
    {
      header: 'Trễ',
      accessor: 'lateCount',
      render: (row) => (row.lateCount > 0 ? <span className="text-orange-600 font-bold">{row.lateCount}</span> : '0'),
    },
    {
      header: 'Vắng',
      accessor: 'absentShifts',
      render: (row) => (row.absentShifts > 0 ? <span className="text-red-500 font-bold">{row.absentShifts}</span> : '0'),
    },
    {
      header: 'Tổng giờ',
      render: (row) => `${(row.totalHoursWorked || 0).toFixed(1)} giờ`,
    },
    {
      header: 'Tỉ lệ đúng giờ',
      render: (row) => {
        const rate = row.attendanceRate || 0;
        return (
          <div className="flex items-center space-x-2 w-32">
            <div className="w-full bg-gray-200 rounded-full h-1.5">
              <div
                className={`h-1.5 rounded-full ${getProgressColor(rate)}`}
                style={{ width: `${rate}%` }}
              ></div>
            </div>
            <span className="text-xs font-bold text-gray-700">{rate.toFixed(0)}%</span>
          </div>
        );
      },
    },
    {
      header: 'Chi tiết',
      className: 'text-right',
      render: (row) => (
        <Button
          variant="secondary"
          className="px-3 py-1.5 text-xs"
          onClick={() => handleViewDetails(row)}
        >
          🔍 Xem chi tiết
        </Button>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      {/* Filters row */}
      <div className="flex flex-col md:flex-row justify-between items-stretch md:items-center bg-surface p-4 rounded-2xl shadow-sm border border-gray-100 space-y-3 md:space-y-0">
        <h1 className="text-xl font-extrabold text-primary">⏰ Quản lý chấm công</h1>
        
        <div className="flex flex-wrap items-center gap-3">
          {/* Year selector */}
          <select
            value={year}
            onChange={(e) => setYear(parseInt(e.target.value, 10))}
            className="px-3 py-2 border rounded-xl outline-none focus:ring-2 focus:ring-accent text-sm bg-white text-gray-800"
          >
            {[nowYear - 2, nowYear - 1, nowYear, nowYear + 1].map((y) => (
              <option key={y} value={y}>Năm {y}</option>
            ))}
          </select>

          {/* Month selector */}
          <select
            value={month}
            onChange={(e) => setMonth(parseInt(e.target.value, 10))}
            className="px-3 py-2 border rounded-xl outline-none focus:ring-2 focus:ring-accent text-sm bg-white text-gray-800"
          >
            {[...Array(12)].map((_, i) => (
              <option key={i + 1} value={i + 1}>Tháng {i + 1}</option>
            ))}
          </select>

          {/* Employee dropdown */}
          <select
            value={employeeId}
            onChange={(e) => setEmployeeId(e.target.value)}
            className="px-3 py-2 border rounded-xl outline-none focus:ring-2 focus:ring-accent text-sm bg-white text-gray-800"
          >
            <option value="ALL">Tất cả nhân viên</option>
            {employees.map((emp) => (
              <option key={emp.userId} value={emp.userId}>{emp.fullName}</option>
            ))}
          </select>

          {/* CSV export */}
          <Button
            onClick={handleExportCSV}
            className="px-4 py-2 text-sm font-semibold flex items-center justify-center space-x-1"
          >
            <span>📥</span> <span>Xuất CSV</span>
          </Button>
        </div>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-5 gap-6">
        <Card title="Nhân viên" value={`${summaryStats.totalStaff} người`} color="primary" icon="👥" />
        <Card title="Tổng số ca" value={`${summaryStats.totalShifts} ca`} color="blue" icon="⏰" />
        <Card title="Đúng giờ" value={`${summaryStats.attendedShifts - summaryStats.lateCount} ca`} color="success" icon="✅" />
        <Card title="Trễ" value={`${summaryStats.lateCount} ca`} color="warning" icon="⚠️" />
        <Card title="Vắng" value={`${summaryStats.absentShifts} ca`} color="danger" icon="🚫" />
      </div>

      {/* Report Table */}
      {error ? (
        <div className="flex flex-col justify-center items-center h-full min-h-[300px] bg-surface rounded-2xl border border-red-100 p-8 shadow-sm">
          <span className="text-4xl mb-3">⚠️</span>
          <p className="text-red-600 font-semibold mb-4">{error}</p>
          <button onClick={fetchFiltersAndData} className="px-5 py-2.5 bg-primary hover:bg-primary-light text-white font-bold rounded-xl transition shadow">
            Tải lại
          </button>
        </div>
      ) : (
        <Table
          columns={columns}
          data={filteredReport}
          loading={loading}
          emptyMessage="Không tìm thấy bản ghi chấm công nào phù hợp"
        />
      )}

      {/* Detailed Records Modal */}
      <Modal isOpen={isDetailOpen} onClose={() => setIsDetailOpen(false)} title={`Lịch sử chấm công: ${detailUser?.fullName}`} size="lg">
        {detailLoading ? (
          <div className="py-12"><Spinner /></div>
        ) : (
          <div className="space-y-4">
            <div className="grid grid-cols-3 gap-4 bg-gray-50 p-4 rounded-xl text-center">
              <div>
                <div className="text-xs text-gray-400">Số ca tham gia</div>
                <div className="text-lg font-bold text-gray-700">{detailUser?.attendedShifts}</div>
              </div>
              <div>
                <div className="text-xs text-gray-400">Số ca đi muộn</div>
                <div className="text-lg font-bold text-orange-600">{detailUser?.lateCount}</div>
              </div>
              <div>
                <div className="text-xs text-gray-400">Tổng giờ làm</div>
                <div className="text-lg font-bold text-primary">{(detailUser?.totalHoursWorked || 0).toFixed(1)}h</div>
              </div>
            </div>

            <Table
              columns={[
                {
                  header: 'Ngày',
                  render: (row) => formatEpoch(row.shiftDate),
                },
                {
                  header: 'Ca',
                  accessor: 'shiftName',
                },
                {
                  header: 'Giờ vào',
                  render: (row) => formatEpochTime(row.checkInAt),
                },
                {
                  header: 'Giờ ra',
                  render: (row) => formatEpochTime(row.checkOutAt),
                },
                {
                  header: 'Tổng giờ',
                  render: (row) => `${(row.durationHours || 0).toFixed(1)}h`,
                },
                {
                  header: 'Trạng thái',
                  render: (row) => getStatusBadge(row.status),
                },
              ]}
              data={detailRecords}
              emptyMessage="Chưa có ca làm việc nào được ghi nhận trong tháng này"
            />
          </div>
        )}
      </Modal>
    </div>
  );
}
