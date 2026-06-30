import axiosInstance from './axiosInstance';

export const getCategories = () =>
  axiosInstance.get('/api/v1/categories');

export const createCategory = (data) =>
  axiosInstance.post('/api/v1/categories', data);

export const updateCategory = (id, data) =>
  axiosInstance.put(`/api/v1/categories/${id}`, data);

export const deleteCategory = (id) =>
  axiosInstance.delete(`/api/v1/categories/${id}`);

export const getProducts = (categoryId = null) =>
  axiosInstance.get('/api/v1/products', {
    params: categoryId && categoryId !== 'ALL' ? { categoryId } : {}
  });

export const createProduct = (data) =>
  axiosInstance.post('/api/v1/products', data);

export const updateProduct = (id, data) =>
  axiosInstance.put(`/api/v1/products/${id}`, data);

// Helper for product active status toggles
export const toggleProduct = (id, product) =>
  axiosInstance.put(`/api/v1/products/${id}`, {
    categoryId: product.categoryId,
    productName: product.productName,
    price: product.price,
    imageUrl: product.imageUrl,
    isActive: !product.isActive,
  });

export const deleteProduct = (id) =>
  axiosInstance.delete(`/api/v1/products/${id}`);
