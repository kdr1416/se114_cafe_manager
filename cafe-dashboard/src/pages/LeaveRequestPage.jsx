import React, { useEffect, useState, useMemo } from 'react';
import { getLeaveRequests } from '../api/leaveApi';
import Table from '../components/ui/Table';
import Badge from '../components/ui/Badge';
import Button from '../components/ui/Button';
import Spinner from '../components/ui/Spinner';
import { formatEpoch } from '../utils/formatDate';
import ApproveModal from '../components/leave/ApproveModal';
import RejectModal from '../components/leave/RejectModal';

export default function LeaveRequestPage() {
  const [activeTab, setActiveTab] = useState('PENDING'); // 'PENDING' | 'APPROVED' | 'REJECTED' | 'ALL'
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Approval/Rejection Modals
  const [isApproveOpen, setIsApproveOpen] = useState(false);
  const [isRejectOpen, setIsRejectOpen] = useState(false);
  const [selectedRequest, setSelectedRequest] = useState(null);

  const fetchLeaveRequestsList = async () => {
    setLoading(true);
    setError('');
    try {
      const res = await getLeaveRequests();
      setRequests(res.data);
    } catch (err) {
      console.error(err);
      setError('Lỗi khi tải danh sách đơn nghỉ phép. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLeaveRequestsList();
  }, []);

  const handleApproveClick = (req) => {
    setSelectedRequest(req);
    setIsApproveOpen(true);
  };

  const handleRejectClick = (req) => {
    setSelectedRequest(req);
    setIsRejectOpen(true);
  };

  // Pending Count
  const pendingCount = useMemo(() => {
    return requests.filter((r) => r.status === 'PENDING').length;
  }, [requests]);

  // Filtered List
  const filteredRequests = useMemo(() => {
    if (activeTab === 'ALL') return requests;
    return requests.filter((r) => r.status === activeTab);
  }, [requests, activeTab]);

  const getStatusBadge = (status) => {
    const badges = {
      PENDING: { label: 'Chờ duyệt', color: 'yellow' },
      APPROVED: { label: 'Đã duyệt', color: 'green' },
      REJECTED: { label: 'Từ chối', color: 'red' },
      CANCELLED: { label: 'Đã hủy', color: 'gray' },
    };
    const b = badges[status] || { label: status, color: 'gray' };
    return <Badge color={b.color}>{b.label}</Badge>;
  };

  // Table Columns
  const columns = [
    {
      header: 'Nhân viên',
      render: (row) => (
        <div>
          <div className="font-bold text-gray-800">{row.userName}</div>
          <div className="text-xs text-gray-400">ID: #{row.userId}</div>
        </div>
      ),
    },
    {
      header: 'Từ ngày',
      render: (row) => formatEpoch(row.startAt),
    },
    {
      header: 'Đến ngày',
      render: (row) => formatEpoch(row.endAt),
    },
    {
      header: 'Lý do nghỉ',
      accessor: 'reason',
      className: 'max-w-xs truncate',
    },
    {
      header: 'Ngày gửi',
      render: (row) => formatEpoch(row.createdAt),
    },
    {
      header: 'Trạng thái',
      render: (row) => getStatusBadge(row.status),
    },
    {
      header: 'Phản hồi',
      render: (row) => (
        row.reviewNote ? (
          <div>
            <div className="text-xs text-gray-400 font-semibold">Bởi: {row.reviewedByName}</div>
            <div className="text-xs text-gray-500 italic truncate max-w-xs">"{row.reviewNote}"</div>
          </div>
        ) : (
          <span className="text-gray-300 text-xs">--</span>
        )
      ),
    },
    {
      header: 'Hành động',
      className: 'text-right',
      render: (row) => (
        row.status === 'PENDING' ? (
          <div className="flex justify-end space-x-2">
            <Button
              variant="success"
              className="px-3 py-1.5 text-xs flex items-center space-x-1"
              onClick={() => handleApproveClick(row)}
            >
              <span>✅</span> <span>Duyệt</span>
            </Button>
            <Button
              variant="danger"
              className="px-3 py-1.5 text-xs flex items-center space-x-1"
              onClick={() => handleRejectClick(row)}
            >
              <span>❌</span> <span>Từ chối</span>
            </Button>
          </div>
        ) : (
          <span className="text-gray-400 text-xs font-semibold">Đã xử lý</span>
        )
      ),
    },
  ];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center bg-surface p-4 rounded-2xl border border-gray-100 shadow-sm">
        <h1 className="text-xl font-extrabold text-primary">📋 Đơn xin nghỉ phép</h1>
      </div>

      {/* Tab Filter bar */}
      <div className="flex border-b border-gray-200 bg-surface rounded-2xl p-2 shadow-sm border border-gray-100">
        <div className="flex space-x-2 w-full overflow-x-auto">
          {[
            { id: 'PENDING', label: 'Chờ duyệt', badge: pendingCount, color: 'bg-yellow-500 text-white' },
            { id: 'APPROVED', label: 'Đã duyệt' },
            { id: 'REJECTED', label: 'Từ chối' },
            { id: 'ALL', label: 'Tất cả' },
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`px-4 py-2.5 text-sm font-semibold rounded-xl transition flex items-center space-x-1.5 flex-shrink-0 ${
                activeTab === tab.id
                  ? 'bg-primary text-white shadow-md'
                  : 'text-gray-500 hover:bg-gray-50 hover:text-gray-700'
              }`}
            >
              <span>{tab.label}</span>
              {tab.badge !== undefined && tab.badge > 0 && (
                <span className="text-xs bg-yellow-500 text-white font-bold rounded-full w-5 h-5 flex items-center justify-center">
                  {tab.badge}
                </span>
              )}
            </button>
          ))}
        </div>
      </div>

      {/* Request Table */}
      {error ? (
        <div className="flex flex-col justify-center items-center h-full min-h-[300px] bg-surface rounded-2xl border border-red-100 p-8 shadow-sm">
          <span className="text-4xl mb-3">⚠️</span>
          <p className="text-red-600 font-semibold mb-4">{error}</p>
          <button onClick={fetchLeaveRequestsList} className="px-5 py-2.5 bg-primary hover:bg-primary-light text-white font-bold rounded-xl transition shadow">
            Tải lại
          </button>
        </div>
      ) : (
        <Table
          columns={columns}
          data={filteredRequests}
          loading={loading}
          emptyMessage="Không tìm thấy đơn xin nghỉ phép nào trong mục này"
        />
      )}

      {/* Approve Modal */}
      <ApproveModal
        isOpen={isApproveOpen}
        onClose={() => setIsApproveOpen(false)}
        request={selectedRequest}
        onConfirm={fetchLeaveRequestsList}
      />

      {/* Reject Modal */}
      <RejectModal
        isOpen={isRejectOpen}
        onClose={() => setIsRejectOpen(false)}
        request={selectedRequest}
        onConfirm={fetchLeaveRequestsList}
      />
    </div>
  );
}
