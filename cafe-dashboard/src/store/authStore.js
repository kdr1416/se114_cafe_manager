import { create } from 'zustand';
import { persist } from 'zustand/middleware';

const useAuthStore = create(
  persist(
    (set) => ({
      token: null,
      user: null,   // { userId, username, fullName, role }
      setAuth: (token, user) => set({ token, user }),
      clearAuth: () => set({ token: null, user: null }),
      isAuthenticated: () => !!localStorage.getItem('token'),
    }),
    { name: 'cafe-auth' }
  )
);

export default useAuthStore;
