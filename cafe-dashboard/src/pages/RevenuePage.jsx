import React, { useEffect, useState, useMemo } from 'react';
import { getMonthlyRevenue, getYearlySummary } from '../api/revenueApi';
import Card from '../components/ui/Card';
import Spinner from '../components/ui/Spinner';
import { formatVnd } from '../utils/formatCurrency';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';

export default function RevenuePage() {
  const tzDate = useMemo(() => new Date(new Date().toLocaleString('en-US', { timeZone: 'Asia/Ho_Chi_Minh' })), []);
  const nowYear = tzDate.getFullYear();
  const nowMonth = tzDate.getMonth() + 1;

  const [viewType, setViewType] = useState('monthly'); // 'monthly' | 'yearly'
  const [currentYear, setCurrentYear] = useState(nowYear);
  const [currentMonth, setCurrentMonth] = useState(nowMonth);
  const [report, setReport] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchRevenueReport = async () => {
    setLoading(true);
    setError('');
    try {
      if (viewType === 'monthly') {
        const res = await getMonthlyRevenue(currentYear, currentMonth);
        setReport(res.data);
      } else {
        const res = await getYearlySummary(currentYear);
        setReport(res.data);
      }
    } catch (err) {
      console.error(err);
      setError('Lỗi khi tải báo cáo doanh thu. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRevenueReport();
  }, [viewType, currentYear, currentMonth]);

  const prevPeriod = () => {
    if (viewType === 'monthly') {
      if (currentMonth === 1) {
        setCurrentMonth(12);
        setCurrentYear(prev => prev - 1);
      } else {
        setCurrentMonth(prev => prev - 1);
      }
    } else {
      setCurrentYear(prev => prev - 1);
    }
  };

  const nextPeriod = () => {
    if (viewType === 'monthly') {
      if (currentYear === nowYear && currentMonth === nowMonth) return; // Prevent future
      if (currentMonth === 12) {
        setCurrentMonth(1);
        setCurrentYear(prev => prev + 1);
      } else {
        setCurrentMonth(prev => prev + 1);
      }
    } else {
      if (currentYear === nowYear) return;
      setCurrentYear(prev => prev + 1);
    }
  };

  const isNextDisabled = useMemo(() => {
    if (viewType === 'monthly') {
      return currentYear >= nowYear && currentMonth >= nowMonth;
    }
    return currentYear >= nowYear;
  }, [viewType, currentYear, currentMonth, nowYear, nowMonth]);

  // Chart data formatting
  const chartData = useMemo(() => {
    if (!report) return [];
    if (viewType === 'monthly') {
      return (report.revenueByDay || []).map(d => ({
        name: `Ngày ${d.day}`,
        'Doanh thu': d.revenue || 0,
        'Đơn hàng': d.orderCount || 0,
      }));
    } else {
      return (report.revenueByMonth || []).map(m => ({
        name: `Tháng ${m.month}`,
        'Doanh thu': m.revenue || 0,
        'Đơn hàng': m.orderCount || 0,
      }));
    }
  }, [report, viewType]);

  // Payment Breakdown Details
  const paymentBreakdown = useMemo(() => {
    if (!report) return { CASH: { rev: 0, count: 0, pct: 0 }, TRANSFER: { rev: 0, count: 0, pct: 0 }, MOMO: { rev: 0, count: 0, pct: 0 } };
    const revs = report.revenueByMethod || {};
    const counts = report.orderCountByMethod || {};
    const total = report.totalRevenue || 1; // avoid division by 0

    const getStats = (method) => {
      const rev = revs[method] || 0;
      const count = counts[method] || 0;
      const pct = (rev / total) * 100;
      return { rev, count, pct };
    };

    return {
      CASH: getStats('CASH'),
      TRANSFER: getStats('TRANSFER'),
      MOMO: getStats('MOMO'),
    };
  }, [report]);

  if (loading) {
    return (
      <div className="flex justify-center items-center h-full min-h-[500px]">
        <Spinner />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Period Navigation Header */}
      <div className="flex flex-col sm:flex-row justify-between items-center bg-surface p-4 rounded-2xl shadow-sm border border-gray-100 space-y-4 sm:space-y-0">
        <div className="flex items-center space-x-4">
          <button
            onClick={prevPeriod}
            className="w-10 h-10 flex items-center justify-center bg-gray-100 hover:bg-gray-200 text-gray-700 font-bold rounded-full transition"
          >
            ←
          </button>
          <span className="text-lg font-extrabold text-primary">
            {viewType === 'monthly' ? `Tháng ${currentMonth}/${currentYear}` : `Năm ${currentYear}`}
          </span>
          <button
            onClick={nextPeriod}
            disabled={isNextDisabled}
            className={`w-10 h-10 flex items-center justify-center text-gray-700 font-bold rounded-full transition ${
              isNextDisabled ? 'bg-gray-50 text-gray-300 cursor-not-allowed' : 'bg-gray-100 hover:bg-gray-200'
            }`}
          >
            →
          </button>
        </div>

        <div className="flex bg-gray-100 p-1 rounded-xl">
          <button
            onClick={() => setViewType('monthly')}
            className={`px-4 py-2 text-sm font-semibold rounded-lg transition ${
              viewType === 'monthly' ? 'bg-white text-primary shadow' : 'text-gray-500 hover:text-gray-800'
            }`}
          >
            Theo Tháng
          </button>
          <button
            onClick={() => setViewType('yearly')}
            className={`px-4 py-2 text-sm font-semibold rounded-lg transition ${
              viewType === 'yearly' ? 'bg-white text-primary shadow' : 'text-gray-500 hover:text-gray-800'
            }`}
          >
            Cả Năm
          </button>
        </div>
      </div>

      {error ? (
        <div className="flex flex-col justify-center items-center h-full min-h-[400px] bg-surface rounded-2xl border border-red-100 p-8 shadow-sm">
          <span className="text-4xl mb-3">⚠️</span>
          <p className="text-red-600 font-semibold mb-4">{error}</p>
          <button onClick={fetchRevenueReport} className="px-5 py-2.5 bg-primary hover:bg-primary-light text-white font-bold rounded-xl transition shadow">
            Thử lại
          </button>
        </div>
      ) : (
        <>
          {/* Summary Row */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-6">
            <Card
              title="Tổng doanh thu"
              value={formatVnd(report?.totalRevenue)}
              color="primary"
              icon="💵"
            />
            <Card
              title="Số đơn hàng"
              value={`${report?.orderCount || 0} đơn`}
              color="accent"
              icon="🛍️"
            />
            <Card
              title="Trung bình/đơn"
              value={formatVnd(report?.avgOrderValue)}
              color="success"
              icon="💳"
            />
            <Card
              title="Kỳ trước"
              value={formatVnd(report?.previousMonthRevenue)}
              color="blue"
              icon="📆"
            />
            <Card
              title="Tăng trưởng"
              value={report?.growthPercent != null ? `${report.growthPercent >= 0 ? '+' : ''}${report.growthPercent.toFixed(1)}%` : '--'}
              color={report?.growthPercent == null ? 'primary' : report.growthPercent >= 0 ? 'success' : 'danger'}
              icon="📈"
              subtitle={
                report?.growthPercent != null ? (
                  <span className={report.growthPercent >= 0 ? 'text-success' : 'text-danger'}>
                    {report.growthPercent >= 0 ? '▲ Tăng trưởng dương' : '▼ Suy giảm'}
                  </span>
                ) : (
                  'Chưa có so sánh'
                )
              }
            />
          </div>

          {/* Daily or Monthly Revenue Chart */}
          <div className="bg-surface rounded-2xl border border-gray-100 p-6 shadow-sm">
            <h3 className="text-lg font-bold text-gray-800 mb-6">
              Biểu đồ doanh thu {viewType === 'monthly' ? `Tháng ${currentMonth}/${currentYear}` : `Năm ${currentYear}`}
            </h3>
            <div className="h-[300px]">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={chartData} margin={{ top: 10, right: 10, left: -25, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#F3F4F6" />
                  <XAxis dataKey="name" tick={{ fill: '#9CA3AF', fontSize: 11 }} axisLine={false} tickLine={false} />
                  <YAxis
                    tickFormatter={(val) => `${(val / 1000000).toFixed(1)}M`}
                    tick={{ fill: '#9CA3AF', fontSize: 11 }}
                    axisLine={false}
                    tickLine={false}
                  />
                  <Tooltip
                    formatter={(value, name, props) => [
                      formatVnd(value),
                      'Doanh thu',
                    ]}
                    labelFormatter={(label) => label}
                    contentStyle={{ background: '#FFF', borderRadius: '12px', border: '1px solid #E5E7EB' }}
                  />
                  <Bar dataKey="Doanh thu" fill="#4B2E1A" radius={[6, 6, 0, 0]} barSize={viewType === 'monthly' ? 16 : 30} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>

          {/* Payment Method Breakdown Cards */}
          <div className="bg-surface rounded-2xl border border-gray-100 p-6 shadow-sm">
            <h3 className="text-lg font-bold text-gray-800 mb-6">Cơ cấu phương thức thanh toán</h3>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              {/* Cash Card */}
              <div className="border border-gray-100 rounded-2xl p-6 bg-gray-50 bg-opacity-50">
                <div className="flex justify-between items-center mb-4">
                  <span className="font-bold text-gray-700">💵 Tiền mặt (CASH)</span>
                  <span className="text-xs bg-gray-200 text-gray-600 px-2 py-0.5 rounded-full font-bold">
                    {paymentBreakdown.CASH.pct.toFixed(1)}%
                  </span>
                </div>
                <div className="text-2xl font-black text-primary mb-2">
                  {formatVnd(paymentBreakdown.CASH.rev)}
                </div>
                <div className="text-xs text-gray-400 mb-4">{paymentBreakdown.CASH.count} giao dịch</div>
                <div className="w-full bg-gray-200 rounded-full h-2">
                  <div
                    className="bg-primary h-2 rounded-full transition-all duration-500"
                    style={{ width: `${paymentBreakdown.CASH.pct}%` }}
                  ></div>
                </div>
              </div>

              {/* Transfer Card */}
              <div className="border border-gray-100 rounded-2xl p-6 bg-gray-50 bg-opacity-50">
                <div className="flex justify-between items-center mb-4">
                  <span className="font-bold text-gray-700">💳 Chuyển khoản (TRANSFER)</span>
                  <span className="text-xs bg-accent bg-opacity-20 text-primary px-2 py-0.5 rounded-full font-bold">
                    {paymentBreakdown.TRANSFER.pct.toFixed(1)}%
                  </span>
                </div>
                <div className="text-2xl font-black text-primary mb-2">
                  {formatVnd(paymentBreakdown.TRANSFER.rev)}
                </div>
                <div className="text-xs text-gray-400 mb-4">{paymentBreakdown.TRANSFER.count} giao dịch</div>
                <div className="w-full bg-gray-200 rounded-full h-2">
                  <div
                    className="bg-accent h-2 rounded-full transition-all duration-500"
                    style={{ width: `${paymentBreakdown.TRANSFER.pct}%` }}
                  ></div>
                </div>
              </div>

              {/* MoMo Card */}
              <div className="border border-gray-100 rounded-2xl p-6 bg-gray-50 bg-opacity-50">
                <div className="flex justify-between items-center mb-4">
                  <span className="font-bold text-gray-700">📱 Ví MoMo (MOMO)</span>
                  <span className="text-xs bg-green-100 text-green-700 px-2 py-0.5 rounded-full font-bold">
                    {paymentBreakdown.MOMO.pct.toFixed(1)}%
                  </span>
                </div>
                <div className="text-2xl font-black text-primary mb-2">
                  {formatVnd(paymentBreakdown.MOMO.rev)}
                </div>
                <div className="text-xs text-gray-400 mb-4">{paymentBreakdown.MOMO.count} giao dịch</div>
                <div className="w-full bg-gray-200 rounded-full h-2">
                  <div
                    className="bg-success h-2 rounded-full transition-all duration-500"
                    style={{ width: `${paymentBreakdown.MOMO.pct}%` }}
                  ></div>
                </div>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
