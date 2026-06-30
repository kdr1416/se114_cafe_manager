import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import LoginPage from './pages/LoginPage';
import Layout from './components/layout/Layout';
import DashboardPage from './pages/DashboardPage';
import EmployeePage from './pages/EmployeePage';
import AttendancePage from './pages/AttendancePage';
import MenuPage from './pages/MenuPage';
import TablePage from './pages/TablePage';
import RevenuePage from './pages/RevenuePage';
import LeaveRequestPage from './pages/LeaveRequestPage';
import ShiftTemplatePage from './pages/ShiftTemplatePage';
import NewsPage from './pages/NewsPage';
import NotFoundPage from './pages/NotFoundPage';

function PrivateRoute({ children }) {
  const token = localStorage.getItem('token');
  return token ? children : <Navigate to="/login" replace />;
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/" element={
          <PrivateRoute>
            <Layout />
          </PrivateRoute>
        }>
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="dashboard" element={<DashboardPage />} />
          <Route path="employees" element={<EmployeePage />} />
          <Route path="attendance" element={<AttendancePage />} />
          <Route path="menu" element={<MenuPage />} />
          <Route path="tables" element={<TablePage />} />
          <Route path="revenue" element={<RevenuePage />} />
          <Route path="leave-requests" element={<LeaveRequestPage />} />
          <Route path="shift-templates" element={<ShiftTemplatePage />} />
          <Route path="news" element={<NewsPage />} />
        </Route>
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
