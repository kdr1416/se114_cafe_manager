import React, { useEffect, useState, useMemo } from 'react';
import { getCategories, deleteCategory, getProducts, toggleProduct, deleteProduct } from '../api/menuApi';
import Button from '../components/ui/Button';
import Badge from '../components/ui/Badge';
import Spinner from '../components/ui/Spinner';
import { formatVnd } from '../utils/formatCurrency';
import { useToast } from '../components/ui/Toast';
import CategoryModal from '../components/menu/CategoryModal';
import ProductModal from '../components/menu/ProductModal';

export default function MenuPage() {
  const { showToast } = useToast();
  
  // Data State
  const [categories, setCategories] = useState([]);
  const [products, setProducts] = useState([]);
  const [selectedCatId, setSelectedCatId] = useState('ALL');
  const [searchTerm, setSearchTerm] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Modals state
  const [isCatOpen, setIsCatOpen] = useState(false);
  const [selectedCat, setSelectedCat] = useState(null);
  
  const [isProdOpen, setIsProdOpen] = useState(false);
  const [selectedProd, setSelectedProd] = useState(null);

  const fetchMenuData = async () => {
    setLoading(true);
    setError('');
    try {
      const [catRes, prodRes] = await Promise.all([
        getCategories(),
        getProducts()
      ]);
      setCategories(catRes.data);
      setProducts(prodRes.data);
    } catch (err) {
      console.error(err);
      setError('Lỗi khi tải dữ liệu Menu. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchMenuData();
  }, []);

  // Category Actions
  const handleAddCategory = () => {
    setSelectedCat(null);
    setIsCatOpen(true);
  };

  const handleEditCategory = (e, cat) => {
    e.stopPropagation(); // Avoid selecting the category tab
    setSelectedCat(cat);
    setIsCatOpen(true);
  };

  const handleDeleteCategory = async (e, cat) => {
    e.stopPropagation();
    if (window.confirm(`Bạn có chắc chắn muốn xóa danh mục "${cat.categoryName}"?\nHành động này cũng sẽ gỡ danh mục khỏi các sản phẩm liên quan.`)) {
      try {
        await deleteCategory(cat.categoryId);
        showToast('Xóa danh mục thành công!', 'success');
        if (selectedCatId === cat.categoryId) setSelectedCatId('ALL');
        fetchMenuData();
      } catch (err) {
        console.error(err);
        showToast(err.response?.data?.message || 'Không thể xóa danh mục.', 'error');
      }
    }
  };

  // Product Actions
  const handleAddProduct = () => {
    setSelectedProd(null);
    setIsProdOpen(true);
  };

  const handleEditProduct = (prod) => {
    setSelectedProd(prod);
    setIsProdOpen(true);
  };

  const handleToggleProductStatus = async (prod) => {
    try {
      await toggleProduct(prod.productId, prod);
      showToast(`Đã ${prod.isActive ? 'tắt' : 'bật'} trạng thái bán sản phẩm "${prod.productName}"`, 'success');
      fetchMenuData();
    } catch (err) {
      console.error(err);
      showToast('Thao tác thay đổi trạng thái thất bại.', 'error');
    }
  };

  const handleDeleteProduct = async (prod) => {
    if (window.confirm(`Bạn có chắc chắn muốn xóa sản phẩm "${prod.productName}"?`)) {
      try {
        await deleteProduct(prod.productId);
        showToast('Xóa sản phẩm thành công!', 'success');
        fetchMenuData();
      } catch (err) {
        console.error(err);
        showToast('Không thể xóa sản phẩm.', 'error');
      }
    }
  };

  // Compute product count per category
  const categoryCounts = useMemo(() => {
    const counts = {};
    products.forEach(p => {
      counts[p.categoryId] = (counts[p.categoryId] || 0) + 1;
    });
    return counts;
  }, [products]);

  // Filter products by search & category
  const filteredProducts = useMemo(() => {
    return products.filter(p => {
      const matchCat = selectedCatId === 'ALL' || p.categoryId === parseInt(selectedCatId, 10);
      const matchSearch = p.productName.toLowerCase().includes(searchTerm.toLowerCase());
      return matchCat && matchSearch;
    });
  }, [products, selectedCatId, searchTerm]);

  // Get category name for product card
  const getCategoryName = (catId) => {
    const cat = categories.find(c => c.categoryId === catId);
    return cat ? cat.categoryName : 'Khác';
  };

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
        <button onClick={fetchMenuData} className="px-5 py-2.5 bg-primary hover:bg-primary-light text-white font-bold rounded-xl transition shadow">
          Thử lại
        </button>
      </div>
    );
  }

  return (
    <div className="flex flex-col md:flex-row space-y-6 md:space-y-0 md:space-x-6 h-[calc(100vh-100px)] overflow-hidden">
      {/* Left panel: Category List (240px / w-60) */}
      <div className="w-full md:w-60 bg-surface border border-gray-100 rounded-2xl flex flex-col h-full overflow-hidden shadow-sm flex-shrink-0">
        <div className="p-4 border-b border-gray-50 flex items-center justify-between">
          <span className="font-extrabold text-primary">📂 Danh mục</span>
          <button
            onClick={handleAddCategory}
            className="w-8 h-8 rounded-full bg-primary bg-opacity-10 text-primary flex items-center justify-center font-bold hover:bg-opacity-20 transition"
            title="Thêm danh mục"
          >
            ＋
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-3 space-y-1">
          {/* Option: All */}
          <button
            onClick={() => setSelectedCatId('ALL')}
            className={`w-full text-left px-4 py-2.5 rounded-xl text-sm font-semibold transition flex items-center justify-between ${
              selectedCatId === 'ALL'
                ? 'bg-primary text-white shadow-sm'
                : 'text-gray-600 hover:bg-gray-50'
            }`}
          >
            <span>Tất cả sản phẩm</span>
            <span className={`text-xs px-2 py-0.5 rounded-full ${
              selectedCatId === 'ALL' ? 'bg-white bg-opacity-30 text-white' : 'bg-gray-100 text-gray-500'
            }`}>
              {products.length}
            </span>
          </button>

          {/* Categories List */}
          {categories.map((cat) => (
            <button
              key={cat.categoryId}
              onClick={() => setSelectedCatId(cat.categoryId)}
              className={`w-full text-left px-4 py-2.5 rounded-xl text-sm font-semibold transition group flex items-center justify-between ${
                selectedCatId === cat.categoryId
                  ? 'bg-primary text-white shadow-sm'
                  : 'text-gray-600 hover:bg-gray-50'
              }`}
            >
              <span className="truncate pr-2">{cat.categoryName}</span>
              <div className="flex items-center space-x-2 flex-shrink-0">
                <span className={`text-xs px-2 py-0.5 rounded-full ${
                  selectedCatId === cat.categoryId ? 'bg-white bg-opacity-30 text-white' : 'bg-gray-100 text-gray-500'
                }`}>
                  {categoryCounts[cat.categoryId] || 0}
                </span>

                {/* Edit/Delete Icons (Visible on Hover in Desktop, or when active) */}
                <div className="hidden group-hover:flex items-center space-x-1.5 pl-1">
                  <span
                    onClick={(e) => handleEditCategory(e, cat)}
                    className="text-xs hover:scale-125 transition duration-150 filter brightness-125 cursor-pointer"
                    title="Sửa"
                  >
                    ✏️
                  </span>
                  <span
                    onClick={(e) => handleDeleteCategory(e, cat)}
                    className="text-xs hover:scale-125 transition duration-150 filter brightness-125 cursor-pointer"
                    title="Xóa"
                  >
                    🗑️
                  </span>
                </div>
              </div>
            </button>
          ))}
        </div>
      </div>

      {/* Right panel: Products List & Grid */}
      <div className="flex-1 bg-surface border border-gray-100 rounded-2xl flex flex-col h-full overflow-hidden shadow-sm">
        {/* Header toolbar */}
        <div className="p-4 border-b border-gray-50 flex flex-col sm:flex-row justify-between items-center space-y-3 sm:space-y-0">
          <div className="w-full sm:w-80 relative">
            <input
              type="text"
              placeholder="Tìm kiếm sản phẩm..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full px-4 py-2 text-sm border rounded-xl outline-none focus:ring-2 focus:ring-accent text-gray-800"
            />
          </div>
          <Button onClick={handleAddProduct} className="w-full sm:w-auto flex items-center justify-center space-x-1.5 py-2">
            <span>➕</span> <span>Thêm sản phẩm</span>
          </Button>
        </div>

        {/* Products Grid */}
        <div className="flex-1 overflow-y-auto p-6 bg-background bg-opacity-30">
          {filteredProducts.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-full text-gray-400 py-12">
              <span className="text-4xl block mb-2">🍽️</span>
              <p className="font-semibold text-sm">Không tìm thấy món ăn/đồ uống nào</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
              {filteredProducts.map((prod) => (
                <div
                  key={prod.productId}
                  className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden flex flex-col justify-between transition hover:shadow-md"
                >
                  <div className="p-4 flex space-x-4">
                    {/* Product Image */}
                    <div className="w-20 h-20 rounded-xl bg-gray-50 border border-gray-100 flex-shrink-0 flex items-center justify-center overflow-hidden">
                      <img
                        src={prod.imageUrl || 'https://placehold.co/150x150?text=CAFE'}
                        alt={prod.productName}
                        onError={(e) => {
                          e.target.onerror = null;
                          e.target.src = 'https://placehold.co/150x150?text=CAFE';
                        }}
                        className="w-full h-full object-cover"
                      />
                    </div>

                    <div className="flex-1 min-w-0">
                      <div className="flex items-center space-x-2 mb-1">
                        <span className={`w-2.5 h-2.5 rounded-full ${prod.isActive ? 'bg-success' : 'bg-gray-300'}`} title={prod.isActive ? 'Đang hoạt động' : 'Tạm khóa'}></span>
                        <h4 className="text-sm font-bold text-gray-800 truncate" title={prod.productName}>
                          {prod.productName}
                        </h4>
                      </div>
                      <div className="text-lg font-black text-primary mb-2">
                        {formatVnd(prod.price)}
                      </div>
                      <Badge color="blue">{getCategoryName(prod.categoryId)}</Badge>
                    </div>
                  </div>

                  {/* Actions Row */}
                  <div className="bg-gray-50 border-t border-gray-100 px-4 py-2.5 flex items-center justify-between">
                    <button
                      onClick={() => handleToggleProductStatus(prod)}
                      className={`text-xs font-bold transition ${
                        prod.isActive ? 'text-red-500 hover:text-red-700' : 'text-success hover:text-opacity-80'
                      }`}
                    >
                      {prod.isActive ? '🛑 Tạm dừng' : '🟢 Kích hoạt'}
                    </button>
                    <div className="flex space-x-3">
                      <button
                        onClick={() => handleEditProduct(prod)}
                        className="text-xs text-blue-600 font-bold hover:text-blue-800"
                      >
                        Sửa
                      </button>
                      <button
                        onClick={() => handleDeleteProduct(prod)}
                        className="text-xs text-red-500 font-bold hover:text-red-700"
                      >
                        Xóa
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Category Modal */}
      <CategoryModal
        isOpen={isCatOpen}
        onClose={() => setIsCatOpen(false)}
        category={selectedCat}
        onSave={fetchMenuData}
      />

      {/* Product Modal */}
      <ProductModal
        isOpen={isProdOpen}
        onClose={() => setIsProdOpen(false)}
        product={selectedProd}
        categories={categories}
        onSave={fetchMenuData}
      />
    </div>
  );
}
