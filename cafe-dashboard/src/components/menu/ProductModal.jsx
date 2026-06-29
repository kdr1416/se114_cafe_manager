import React, { useState, useEffect } from 'react';
import Modal from '../ui/Modal';
import { createProduct, updateProduct } from '../../api/menuApi';
import { uploadImage } from '../../api/uploadApi';
import { useToast } from '../ui/Toast';

export default function ProductModal({ isOpen, onClose, product, categories = [], onSave }) {
  const isEdit = !!product;
  const { showToast } = useToast();

  const [productName, setProductName] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [price, setPrice] = useState('');
  const [imageUrl, setImageUrl] = useState('');
  const [imageFile, setImageFile] = useState(null);
  const [isActive, setIsActive] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (product) {
      setProductName(product.productName || '');
      setCategoryId(product.categoryId || '');
      setPrice(product.price || '');
      setImageUrl(product.imageUrl || '');
      setIsActive(product.isActive ?? true);
    } else {
      setProductName('');
      setCategoryId(categories[0]?.categoryId || '');
      setPrice('');
      setImageUrl('');
      setIsActive(true);
    }
    setImageFile(null);
    setError('');
  }, [product, categories, isOpen]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!productName.trim()) {
      setError('Tên sản phẩm không được để trống.');
      return;
    }
    if (!categoryId) {
      setError('Vui lòng chọn danh mục cho sản phẩm.');
      return;
    }
    if (price === '' || parseFloat(price) < 0) {
      setError('Giá sản phẩm không hợp lệ.');
      return;
    }

    setLoading(true);
    try {
      let finalImageUrl = imageUrl;
      if (imageFile) {
        try {
          finalImageUrl = await uploadImage(imageFile, 'products');
        } catch (uploadErr) {
          console.error(uploadErr);
          setError('Không thể tải hình ảnh lên Storage. Vui lòng kiểm tra cấu hình Supabase.');
          setLoading(false);
          return;
        }
      }

      const payload = {
        productName: productName.trim(),
        categoryId: parseInt(categoryId, 10),
        price: parseFloat(price),
        imageUrl: finalImageUrl || null,
        isActive,
      };

      if (isEdit) {
        await updateProduct(product.productId, payload);
        showToast('Cập nhật sản phẩm thành công!', 'success');
      } else {
        await createProduct(payload);
        showToast('Tạo sản phẩm mới thành công!', 'success');
      }
      onSave();
      onClose();
    } catch (err) {
      console.error(err);
      setError(err.response?.data?.message || 'Có lỗi xảy ra. Vui lòng kiểm tra lại thông tin.');
    } finally {
      setLoading(false);
    }
  };

  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      setImageFile(file);
      setImageUrl(URL.createObjectURL(file)); // Local preview url
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={isEdit ? 'Chỉnh sửa sản phẩm' : 'Thêm sản phẩm mới'}>
      <form onSubmit={handleSubmit} className="space-y-4">
        {error && (
          <div className="p-3 bg-red-50 text-red-600 rounded-lg text-sm border-l-4 border-danger">
            {error}
          </div>
        )}

        <div>
          <label className="block text-sm font-semibold text-gray-700 mb-1">Tên sản phẩm *</label>
          <input
            type="text"
            value={productName}
            onChange={(e) => setProductName(e.target.value)}
            placeholder="Ví dụ: Cà phê sữa đá..."
            className="w-full px-4 py-2 border rounded-xl outline-none focus:ring-2 focus:ring-accent text-gray-800"
            disabled={loading}
            required
          />
        </div>

        <div>
          <label className="block text-sm font-semibold text-gray-700 mb-1">Danh mục *</label>
          <select
            value={categoryId}
            onChange={(e) => setCategoryId(e.target.value)}
            className="w-full px-4 py-2 border rounded-xl outline-none focus:ring-2 focus:ring-accent bg-white text-gray-800"
            disabled={loading}
            required
          >
            <option value="" disabled>-- Chọn danh mục --</option>
            {categories.map((cat) => (
              <option key={cat.categoryId} value={cat.categoryId}>
                {cat.categoryName}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label className="block text-sm font-semibold text-gray-700 mb-1">Giá bán (VND) *</label>
          <input
            type="number"
            value={price}
            onChange={(e) => setPrice(e.target.value)}
            placeholder="Ví dụ: 29000"
            className="w-full px-4 py-2 border rounded-xl outline-none focus:ring-2 focus:ring-accent text-gray-800"
            disabled={loading}
            min="0"
            required
          />
        </div>

        <div>
          <label className="block text-sm font-semibold text-gray-700 mb-1">Hình ảnh sản phẩm</label>
          <div className="flex items-center space-x-4">
            <input
              type="file"
              accept="image/*"
              onChange={handleFileChange}
              className="hidden"
              id="product-image-upload"
              disabled={loading}
            />
            <label
              htmlFor="product-image-upload"
              className={`px-4 py-2 border border-dashed border-gray-300 rounded-xl hover:bg-gray-50 transition cursor-pointer text-sm font-semibold text-gray-600 ${
                loading ? 'opacity-50 cursor-not-allowed' : ''
              }`}
            >
              Chọn ảnh từ thiết bị
            </label>
            {imageFile && (
              <span className="text-xs text-gray-400 max-w-[200px] truncate">
                {imageFile.name}
              </span>
            )}
          </div>
          {imageUrl && (
            <div className="mt-3 border rounded-xl overflow-hidden w-24 h-24 bg-gray-50 flex items-center justify-center relative group">
              <img
                src={imageUrl}
                alt="Product preview"
                onError={(e) => {
                  e.target.onerror = null;
                  e.target.src = 'https://placehold.co/100x100?text=Loi+anh';
                }}
                className="object-cover w-full h-full"
              />
            </div>
          )}
        </div>

        <div className="flex items-center space-x-2 py-2">
          <input
            type="checkbox"
            id="isActiveProd"
            checked={isActive}
            onChange={(e) => setIsActive(e.target.checked)}
            className="w-4 h-4 text-primary focus:ring-accent border-gray-300 rounded"
            disabled={loading}
          />
          <label htmlFor="isActiveProd" className="text-sm font-semibold text-gray-700 select-none">
            Cho phép hoạt động (Có thể order trên menu)
          </label>
        </div>

        <div className="flex justify-end space-x-3 pt-4 border-t border-gray-100">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 text-sm font-medium text-gray-500 hover:bg-gray-100 rounded-xl transition"
            disabled={loading}
          >
            Hủy
          </button>
          <button
            type="submit"
            className="px-4 py-2 text-sm font-semibold text-white bg-primary hover:bg-primary-light rounded-xl shadow transition"
            disabled={loading}
          >
            {loading ? 'Đang lưu...' : 'Lưu sản phẩm'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
