import React, { useEffect, useState, useMemo } from 'react';
import { getMonthlyRevenue } from '../api/revenueApi';
import { getShifts } from '../api/shiftApi';
import Card from '../components/ui/Card';
import Spinner from '../components/ui/Spinner';
import { formatVnd } from '../utils/formatCurrency';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts';

export default function DashboardPage() {
  const [activeTab, setActiveTab] = useState('month'); // 'today' | 'week' | 'month'
  const [revenueData, setRevenueData] = useState(null);
  const [shifts, setShifts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const todayStr = useMemo(() => {
    const tzDate = new Date(new Date().toLocaleString('en-US', { timeZone: 'Asia/Ho_Chi_Minh' }));
    const yyyy = tzDate.getFullYear();
    const mm = String(tzDate.getMonth() + 1).padStart(2, '0');
    const dd = String(tzDate.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  }, []);

  const currentDayNum = useMemo(() => {
    const tzDate = new Date(new Date().toLocaleString('en-US', { timeZone: 'Asia/Ho_Chi_Minh' }));
    return tzDate.getDate();
  }, []);

  const fetchData = async () => {
    setLoading(true);
    setError('');
    try {
      const tzDate = new Date(new Date().toLocaleString('en-US', { timeZone: 'Asia/Ho_Chi_Minh' }));
      const year = tzDate.getFullYear();
      const month = tzDate.getMonth() + 1;

      // Parallel data fetching
      const [revRes, shiftRes] = await Promise.all([
        getMonthlyRevenue(year, month),
        getShifts({ date: todayStr })
      ]);

      setRevenueData(revRes.data);
      setShifts(shiftRes.data);
    } catch (err) {
      console.error(err);
      setError('Không thể tải dữ liệu báo cáo. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [todayStr]);

  // Client-side calculations based on activeTab
  const stats = useMemo(() => {
    if (!revenueData) return { revenue: 0, orders: 0, avgValue: 0, growth: 0 };

    const dailyList = revenueData.revenueByDay || [];

    if (activeTab === 'today') {
      const todayRecord = dailyList.find(d => d.day === currentDayNum) || { revenue: 0, orderCount: 0 };
      const rev = todayRecord.revenue || 0;
      const ords = todayRecord.orderCount || 0;
      return {
        revenue: rev,
        orders: ords,
        avgValue: ords > 0 ? rev / ords : 0,
        growth: null // Not applicable for single day vs full month comparison
      };
    }

    if (activeTab === 'week') {
      // Filter last 7 days
      const last7Days = dailyList.filter(d => d.day > currentDayNum - 7 && d.day <= currentDayNum);
      const rev = last7Days.reduce((sum, d) => sum + (d.revenue || 0), 0);
      const ords = last7Days.reduce((sum, d) => sum + (d.orderCount || 0), 0);
      return {
        revenue: rev,
        orders: ords,
        avgValue: ords > 0 ? rev / ords : 0,
        growth: null
      };
    }

    // Default to 'month'
    return {
      revenue: revenueData.totalRevenue || 0,
      orders: revenueData.orderCount || 0,
      avgValue: revenueData.avgOrderValue || 0,
      growth: revenueData.growthPercent || 0
    };
  }, [revenueData, activeTab, currentDayNum]);

  // Active shifts: PUBLISHED, IN_PROGRESS, or CLOSED
  const activeShiftsCount = useMemo(() => {
    return shifts.filter(s => ['PUBLISHED', 'IN_PROGRESS', 'CLOSED'].includes(s.status)).length;
  }, [shifts]);

  // Ca đang chạy (IN_PROGRESS)
  const inProgressCount = useMemo(() => {
    return shifts.filter(s => s.status === 'IN_PROGRESS').length;
  }, [shifts]);

  // Ca sắp tới (PUBLISHED)
  const publishedCount = useMemo(() => {
    return shifts.filter(s => s.status === 'PUBLISHED').length;
  }, [shifts]);

  // Ca đã đóng (CLOSED)
  const closedCount = useMemo(() => {
    return shifts.filter(s => s.status === 'CLOSED').length;
  }, [shifts]);

  // Danh sách phân rã trạng thái ca làm việc
  const shiftSubtitle = useMemo(() => {
    return (
      <div className="mt-2 space-y-1.5 text-xs text-gray-500 font-semibold w-full">
        <div className="flex justify-between border-b border-gray-50 pb-1">
          <span>🏃 Ca đang chạy:</span>
          <span className="text-gray-800">{inProgressCount} ca</span>
        </div>
        <div className="flex justify-between border-b border-gray-50 pb-1">
          <span>⏳ Ca sắp tới:</span>
          <span className="text-accent">{publishedCount} ca</span>
        </div>
        <div className="flex justify-between">
          <span>✅ Ca đã đóng:</span>
          <span className="text-success">{closedCount} ca</span>
        </div>
      </div>
    );
  }, [inProgressCount, publishedCount, closedCount]);

  // Prepare Chart Data
  const chartData = useMemo(() => {
    if (!revenueData || !revenueData.revenueByDay) return [];
    return revenueData.revenueByDay.map(d => ({
      name: `Ngày ${d.day}`,
      'Doanh thu': d.revenue || 0,
      'Đơn hàng': d.orderCount || 0,
    }));
  }, [revenueData]);

  // Prepare Pie Chart Data for payment methods
  const paymentChartData = useMemo(() => {
    if (!revenueData || !revenueData.revenueByMethod) return [];
    return Object.entries(revenueData.revenueByMethod).map(([key, val]) => {
      let name = 'MOMO';
      if (key === 'CASH') name = 'Tiền mặt';
      if (key === 'TRANSFER') name = 'Chuyển khoản';
      return { name, value: val };
    });
  }, [revenueData]);

  const COLORS = ['#4B2E1A', '#D4A055', '#22C55E'];

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
        <button onClick={fetchData} className="px-5 py-2.5 bg-primary hover:bg-primary-light text-white font-bold rounded-xl transition shadow">
          Thử lại
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Tab Filter Selector */}
      <div className="flex justify-between items-center bg-surface p-2 rounded-2xl shadow-sm border border-gray-100">
        <h1 className="text-lg font-extrabold text-primary pl-4">☕ Trang điều hành</h1>
        <div className="flex space-x-1">
          {['today', 'week', 'month'].map((tab) => {
            const labels = { today: 'Hôm nay', week: '7 ngày qua', month: 'Tháng này' };
            return (
              <button
                key={tab}
                onClick={() => setActiveTab(tab)}
                className={`px-4 py-2 text-sm font-semibold rounded-xl transition duration-150 ${
                  activeTab === tab
                    ? 'bg-primary text-white shadow-sm'
                    : 'text-gray-500 hover:bg-gray-50 hover:text-gray-700'
                }`}
              >
                {labels[tab]}
              </button>
            );
          })}
        </div>
      </div>

      {/* Stats Cards Row */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <Card
          title="Doanh thu"
          value={formatVnd(stats.revenue)}
          color="primary"
          icon="💰"
          subtitle={
            stats.growth !== null ? (
              <span className={stats.growth >= 0 ? 'text-success' : 'text-danger'}>
                {stats.growth >= 0 ? '▲' : '▼'} {Math.abs(stats.growth).toFixed(1)}% so với tháng trước
              </span>
            ) : (
              'Doanh thu thực nhận'
            )
          }
        />
        <Card
          title="Số đơn hàng"
          value={`${stats.orders} đơn`}
          color="accent"
          icon="📦"
          subtitle="Số đơn hoàn thành"
        />
        <Card
          title="Trung bình / Đơn"
          value={formatVnd(stats.avgValue)}
          color="success"
          icon="📊"
          subtitle="Doanh thu bình quân mỗi đơn"
        />
        <Card
          title="Ca làm việc hôm nay"
          value={`${activeShiftsCount} ca`}
          color="warning"
          icon="⏰"
          subtitle={shiftSubtitle}
        />
      </div>

      {/* Revenue Chart */}
      <div className="bg-surface rounded-2xl border border-gray-100 p-6 shadow-sm">
        <h3 className="text-lg font-bold text-gray-800 mb-6">
          Doanh thu theo ngày — Tháng {revenueData?.month}/{revenueData?.year}
        </h3>
        <div className="h-[320px]">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={chartData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#F3F4F6" />
              <XAxis dataKey="name" tick={{ fill: '#9CA3AF', fontSize: 11 }} axisLine={false} tickLine={false} />
              <YAxis
                tickFormatter={(val) => `${(val / 1000000).toFixed(1)}M`}
                tick={{ fill: '#9CA3AF', fontSize: 11 }}
                axisLine={false}
                tickLine={false}
              />
              <Tooltip
                formatter={(value) => [formatVnd(value), 'Doanh thu']}
                contentStyle={{ background: '#FFF', borderRadius: '12px', border: '1px solid #E5E7EB', boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)' }}
              />
              <Bar dataKey="Doanh thu" fill="#4B2E1A" radius={[8, 8, 0, 0]} barSize={20} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Bottom widgets */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Top Sold Products */}
        <div className="bg-surface rounded-2xl border border-gray-100 p-6 shadow-sm flex flex-col justify-between">
          <div>
            <h3 className="text-lg font-bold text-gray-800 mb-6">Top sản phẩm bán chạy</h3>
            <div className="space-y-4">
              {revenueData?.itemsSold?.slice(0, 5).map((item, idx) => (
                <div key={item.productId} className="flex items-center justify-between py-2 border-b border-gray-50 last:border-0">
                  <div className="flex items-center space-x-3">
                    <span className={`w-6 h-6 rounded-full flex items-center justify-center font-bold text-xs ${
                      idx === 0 ? 'bg-accent text-primary' : 'bg-gray-100 text-gray-500'
                    }`}>
                      {idx + 1}
                    </span>
                    <span className="font-semibold text-gray-700 text-sm">{item.productName}</span>
                  </div>
                  <div className="text-right">
                    <div className="text-sm font-bold text-gray-800">{formatVnd(item.revenue)}</div>
                    <div className="text-xs text-gray-400">{item.quantity} sản phẩm</div>
                  </div>
                </div>
              )) || <div className="text-center text-gray-400 py-6">Không có dữ liệu bán hàng</div>}
            </div>
          </div>
        </div>

        {/* Payment Methods Breakdown */}
        <div className="bg-surface rounded-2xl border border-gray-100 p-6 shadow-sm flex flex-col items-center">
          <div className="w-full text-left">
            <h3 className="text-lg font-bold text-gray-800 mb-6">Phương thức thanh toán</h3>
          </div>
          {paymentChartData.length > 0 ? (
            <div className="flex flex-col sm:flex-row items-center justify-center w-full space-y-4 sm:space-y-0 sm:space-x-8">
              <div className="w-48 h-48 relative">
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={paymentChartData}
                      cx="50%"
                      cy="50%"
                      innerRadius={60}
                      outerRadius={85}
                      paddingAngle={3}
                      dataKey="value"
                    >
                      {paymentChartData.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                      ))}
                    </Pie>
                    <Tooltip formatter={(value) => formatVnd(value)} />
                  </PieChart>
                </ResponsiveContainer>
              </div>
              <div className="space-y-3">
                {paymentChartData.map((item, idx) => (
                  <div key={idx} className="flex items-center space-x-3 text-sm">
                    <div className="w-3 h-3 rounded-full" style={{ backgroundColor: COLORS[idx % COLORS.length] }}></div>
                    <span className="text-gray-500 font-medium">{item.name}:</span>
                    <span className="font-bold text-gray-800">{formatVnd(item.value)}</span>
                  </div>
                ))}
              </div>
            </div>
          ) : (
            <div className="text-center text-gray-400 py-12">Không có dữ liệu giao dịch</div>
          )}
        </div>
      </div>
    </div>
  );
}
