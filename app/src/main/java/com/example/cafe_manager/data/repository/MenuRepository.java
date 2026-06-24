package com.example.cafe_manager.data.repository;

import android.content.Context;
import android.widget.Toast;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cafe_manager.data.local.entity.CategoryEntity;
import com.example.cafe_manager.data.local.entity.ProductEntity;
import com.example.cafe_manager.data.remote.ApiClient;
import com.example.cafe_manager.data.remote.MenuApiService;
import com.example.cafe_manager.data.remote.NetworkException;
import com.example.cafe_manager.util.AppExecutors;
import com.example.cafe_manager.util.RepositoryCallback;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository quản lý danh mục và món ăn, kết nối trực tiếp đến Backend qua Retrofit APIs.
 */
public class MenuRepository {

    private static MenuRepository instance;

    private final Context context;
    private final MenuApiService menuApiService;
    private final AppExecutors appExecutors;

    private final MutableLiveData<List<CategoryEntity>> activeCategoriesLive;
    private final MutableLiveData<List<CategoryEntity>> allCategoriesLive;
    private final MutableLiveData<List<ProductEntity>> activeProductsLive;
    private final MutableLiveData<List<ProductEntity>> allProductsLive;

    public static synchronized MenuRepository getInstance(Context context) {
        if (instance == null) {
            instance = new MenuRepository(context);
        }
        return instance;
    }

    private MenuRepository(Context context) {
        this.context = context.getApplicationContext();
        this.menuApiService = ApiClient.getInstance(context).getService(MenuApiService.class);
        this.appExecutors = AppExecutors.getInstance();

        this.activeCategoriesLive = new MutableLiveData<>();
        this.allCategoriesLive = new MutableLiveData<>();
        this.activeProductsLive = new MutableLiveData<>();
        this.allProductsLive = new MutableLiveData<>();

        refreshActiveCategories();
        refreshAllCategories();
        refreshActiveProducts();
        refreshAllProducts();
    }

    public LiveData<List<CategoryEntity>> getActiveCategories() {
        return activeCategoriesLive;
    }

    public LiveData<List<CategoryEntity>> getAllCategories() {
        return allCategoriesLive;
    }

    public LiveData<List<ProductEntity>> getActiveProducts() {
        return activeProductsLive;
    }

    public LiveData<List<ProductEntity>> getAllProducts() {
        return allProductsLive;
    }

    public LiveData<List<ProductEntity>> getProductsByCategory(int categoryId) {
        MutableLiveData<List<ProductEntity>> liveData = new MutableLiveData<>();
        menuApiService.getProducts(categoryId, true).enqueue(new Callback<List<ProductEntity>>() {
            @Override
            public void onResponse(Call<List<ProductEntity>> call, Response<List<ProductEntity>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    liveData.postValue(response.body());
                } else {
                    showError(parseError(response));
                }
            }

            @Override
            public void onFailure(Call<List<ProductEntity>> call, Throwable t) {
                showError(new NetworkException("Không có kết nối mạng", t));
            }
        });
        return liveData;
    }

    public LiveData<List<ProductEntity>> getProductsByCategoryIncludingInactive(int categoryId) {
        MutableLiveData<List<ProductEntity>> liveData = new MutableLiveData<>();
        menuApiService.getProducts(categoryId, null).enqueue(new Callback<List<ProductEntity>>() {
            @Override
            public void onResponse(Call<List<ProductEntity>> call, Response<List<ProductEntity>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    liveData.postValue(response.body());
                } else {
                    showError(parseError(response));
                }
            }

            @Override
            public void onFailure(Call<List<ProductEntity>> call, Throwable t) {
                showError(new NetworkException("Không có kết nối mạng", t));
            }
        });
        return liveData;
    }

    public void refreshActiveCategories() {
        menuApiService.getActiveCategories().enqueue(new Callback<List<CategoryEntity>>() {
            @Override
            public void onResponse(Call<List<CategoryEntity>> call, Response<List<CategoryEntity>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    activeCategoriesLive.postValue(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<CategoryEntity>> call, Throwable t) {
                // Thất bại im lặng
            }
        });
    }

    public void refreshAllCategories() {
        menuApiService.getActiveCategories().enqueue(new Callback<List<CategoryEntity>>() {
            @Override
            public void onResponse(Call<List<CategoryEntity>> call, Response<List<CategoryEntity>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allCategoriesLive.postValue(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<CategoryEntity>> call, Throwable t) {
                // Thất bại im lặng
            }
        });
    }

    public void refreshActiveProducts() {
        menuApiService.getProducts(null, true).enqueue(new Callback<List<ProductEntity>>() {
            @Override
            public void onResponse(Call<List<ProductEntity>> call, Response<List<ProductEntity>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    activeProductsLive.postValue(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<ProductEntity>> call, Throwable t) {
                // Thất bại im lặng
            }
        });
    }

    public void refreshAllProducts() {
        menuApiService.getProducts(null, null).enqueue(new Callback<List<ProductEntity>>() {
            @Override
            public void onResponse(Call<List<ProductEntity>> call, Response<List<ProductEntity>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allProductsLive.postValue(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<ProductEntity>> call, Throwable t) {
                // Thất bại im lặng
            }
        });
    }

    public void insertProduct(ProductEntity product, RepositoryCallback<ProductEntity> callback) {
        menuApiService.createProduct(product).enqueue(new Callback<ProductEntity>() {
            @Override
            public void onResponse(Call<ProductEntity> call, Response<ProductEntity> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ProductEntity newProduct = response.body();
                    appExecutors.mainThread().execute(() -> {
                        List<ProductEntity> currentAll = allProductsLive.getValue();
                        if (currentAll != null) {
                            List<ProductEntity> newAll = new java.util.ArrayList<>(currentAll);
                            newAll.add(newProduct);
                            allProductsLive.setValue(newAll);
                        } else {
                            refreshAllProducts();
                        }

                        if (newProduct.isActive()) {
                            List<ProductEntity> currentActive = activeProductsLive.getValue();
                            if (currentActive != null) {
                                List<ProductEntity> newActive = new java.util.ArrayList<>(currentActive);
                                newActive.add(newProduct);
                                activeProductsLive.setValue(newActive);
                            } else {
                                refreshActiveProducts();
                            }
                        }
                    });
                    if (callback != null) {
                        appExecutors.mainThread().execute(() -> callback.onSuccess(newProduct));
                    }
                } else {
                    Exception e = parseError(response);
                    showError(e);
                    if (callback != null) {
                        appExecutors.mainThread().execute(() -> callback.onError(e));
                    }
                }
            }

            @Override
            public void onFailure(Call<ProductEntity> call, Throwable t) {
                Exception e = new NetworkException("Không có kết nối mạng", t);
                showError(e);
                if (callback != null) {
                    appExecutors.mainThread().execute(() -> callback.onError(e));
                }
            }
        });
    }

    @Deprecated
    public void insertProduct(ProductEntity product) {
        insertProduct(product, null);
    }

    public void updateProduct(ProductEntity product, RepositoryCallback<ProductEntity> callback) {
        menuApiService.updateProduct(product.getProductId(), product).enqueue(new Callback<ProductEntity>() {
            @Override
            public void onResponse(Call<ProductEntity> call, Response<ProductEntity> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ProductEntity updatedProduct = response.body();
                    appExecutors.mainThread().execute(() -> {
                        // Cập nhật allProductsLive
                        List<ProductEntity> currentAll = allProductsLive.getValue();
                        if (currentAll != null) {
                            List<ProductEntity> newAll = new java.util.ArrayList<>(currentAll);
                            for (int i = 0; i < newAll.size(); i++) {
                                if (newAll.get(i).getProductId() == updatedProduct.getProductId()) {
                                    newAll.set(i, updatedProduct);
                                    break;
                                }
                            }
                            allProductsLive.setValue(newAll);
                        } else {
                            refreshAllProducts();
                        }

                        // Cập nhật activeProductsLive
                        List<ProductEntity> currentActive = activeProductsLive.getValue();
                        if (currentActive != null) {
                            List<ProductEntity> newActive = new java.util.ArrayList<>(currentActive);
                            boolean found = false;
                            for (int i = 0; i < newActive.size(); i++) {
                                if (newActive.get(i).getProductId() == updatedProduct.getProductId()) {
                                    if (updatedProduct.isActive()) {
                                        newActive.set(i, updatedProduct);
                                    } else {
                                        newActive.remove(i);
                                    }
                                    found = true;
                                    break;
                                }
                            }
                            if (!found && updatedProduct.isActive()) {
                                newActive.add(updatedProduct);
                            }
                            activeProductsLive.setValue(newActive);
                        } else {
                            refreshActiveProducts();
                        }
                    });
                    if (callback != null) {
                        appExecutors.mainThread().execute(() -> callback.onSuccess(updatedProduct));
                    }
                } else {
                    Exception e = parseError(response);
                    showError(e);
                    if (callback != null) {
                        appExecutors.mainThread().execute(() -> callback.onError(e));
                    }
                }
            }

            @Override
            public void onFailure(Call<ProductEntity> call, Throwable t) {
                Exception e = new NetworkException("Không có kết nối mạng", t);
                showError(e);
                if (callback != null) {
                    appExecutors.mainThread().execute(() -> callback.onError(e));
                }
            }
        });
    }

    @Deprecated
    public void updateProduct(ProductEntity product) {
        updateProduct(product, null);
    }

    public void updateProductActiveStatus(int productId, boolean isActive) {
        // Tìm và sửa trực tiếp trong cache để không cần request GET chi tiết
        ProductEntity targetProduct = null;
        if (allProductsLive.getValue() != null) {
            for (ProductEntity p : allProductsLive.getValue()) {
                if (p.getProductId() == productId) {
                    targetProduct = p;
                    break;
                }
            }
        }

        if (targetProduct != null) {
            ProductEntity updateProd = new ProductEntity(
                    targetProduct.getCategoryId(),
                    targetProduct.getProductName(),
                    targetProduct.getPrice(),
                    targetProduct.getImageUrl(),
                    isActive,
                    targetProduct.getCreatedAt()
            );
            updateProd.setProductId(targetProduct.getProductId());
            updateProduct(updateProd);
        } else {
            // Fallback nếu cache rỗng
            menuApiService.getProductById(productId).enqueue(new Callback<ProductEntity>() {
                @Override
                public void onResponse(Call<ProductEntity> call, Response<ProductEntity> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ProductEntity product = response.body();
                        product.setActive(isActive);
                        updateProduct(product);
                    } else {
                        showError(parseError(response));
                    }
                }

                @Override
                public void onFailure(Call<ProductEntity> call, Throwable t) {
                    showError(new NetworkException("Không có kết nối mạng", t));
                }
            });
        }
    }

    public void insertCategory(CategoryEntity category, RepositoryCallback<CategoryEntity> callback) {
        menuApiService.createCategory(category).enqueue(new Callback<CategoryEntity>() {
            @Override
            public void onResponse(Call<CategoryEntity> call, Response<CategoryEntity> response) {
                if (response.isSuccessful() && response.body() != null) {
                    CategoryEntity newCategory = response.body();
                    appExecutors.mainThread().execute(() -> {
                        // Thêm vào allCategoriesLive
                        List<CategoryEntity> currentAll = allCategoriesLive.getValue();
                        if (currentAll != null) {
                            List<CategoryEntity> newAll = new java.util.ArrayList<>(currentAll);
                            newAll.add(newCategory);
                            allCategoriesLive.setValue(newAll);
                        } else {
                            refreshAllCategories();
                        }

                        // Thêm vào activeCategoriesLive (vì danh mục mới mặc định active)
                        List<CategoryEntity> currentActive = activeCategoriesLive.getValue();
                        if (currentActive != null) {
                            List<CategoryEntity> newActive = new java.util.ArrayList<>(currentActive);
                            newActive.add(newCategory);
                            activeCategoriesLive.setValue(newActive);
                        } else {
                            refreshActiveCategories();
                        }
                    });
                    if (callback != null) {
                        appExecutors.mainThread().execute(() -> callback.onSuccess(newCategory));
                    }
                } else {
                    Exception e = parseError(response);
                    showError(e);
                    if (callback != null) {
                        appExecutors.mainThread().execute(() -> callback.onError(e));
                    }
                }
            }

            @Override
            public void onFailure(Call<CategoryEntity> call, Throwable t) {
                Exception e = new NetworkException("Không có kết nối mạng", t);
                showError(e);
                if (callback != null) {
                    appExecutors.mainThread().execute(() -> callback.onError(e));
                }
            }
        });
    }

    @Deprecated
    public void insertCategory(CategoryEntity category, Runnable onSuccess) {
        insertCategory(category, new RepositoryCallback<CategoryEntity>() {
            @Override
            public void onSuccess(CategoryEntity result) {
                if (onSuccess != null) {
                    appExecutors.mainThread().execute(onSuccess);
                }
            }
            @Override
            public void onError(Exception e) {}
        });
    }

    public void updateCategory(CategoryEntity category, RepositoryCallback<CategoryEntity> callback) {
        menuApiService.updateCategory(category.getCategoryId(), category).enqueue(new Callback<CategoryEntity>() {
            @Override
            public void onResponse(Call<CategoryEntity> call, Response<CategoryEntity> response) {
                if (response.isSuccessful() && response.body() != null) {
                    CategoryEntity updatedCategory = response.body();
                    appExecutors.mainThread().execute(() -> {
                        // Cập nhật allCategoriesLive
                        List<CategoryEntity> currentAll = allCategoriesLive.getValue();
                        if (currentAll != null) {
                            List<CategoryEntity> newAll = new java.util.ArrayList<>(currentAll);
                            for (int i = 0; i < newAll.size(); i++) {
                                if (newAll.get(i).getCategoryId() == updatedCategory.getCategoryId()) {
                                    newAll.set(i, updatedCategory);
                                    break;
                                }
                            }
                            allCategoriesLive.setValue(newAll);
                        } else {
                            refreshAllCategories();
                        }

                        // Cập nhật activeCategoriesLive
                        List<CategoryEntity> currentActive = activeCategoriesLive.getValue();
                        if (currentActive != null) {
                            List<CategoryEntity> newActive = new java.util.ArrayList<>(currentActive);
                            for (int i = 0; i < newActive.size(); i++) {
                                if (newActive.get(i).getCategoryId() == updatedCategory.getCategoryId()) {
                                    newActive.set(i, updatedCategory);
                                    break;
                                }
                            }
                            activeCategoriesLive.setValue(newActive);
                        } else {
                            refreshActiveCategories();
                        }
                    });
                    if (callback != null) {
                        appExecutors.mainThread().execute(() -> callback.onSuccess(updatedCategory));
                    }
                } else {
                    Exception e = parseError(response);
                    showError(e);
                    if (callback != null) {
                        appExecutors.mainThread().execute(() -> callback.onError(e));
                    }
                }
            }

            @Override
            public void onFailure(Call<CategoryEntity> call, Throwable t) {
                Exception e = new NetworkException("Không có kết nối mạng", t);
                showError(e);
                if (callback != null) {
                    appExecutors.mainThread().execute(() -> callback.onError(e));
                }
            }
        });
    }

    @Deprecated
    public void updateCategory(CategoryEntity category, Runnable onSuccess) {
        updateCategory(category, new RepositoryCallback<CategoryEntity>() {
            @Override
            public void onSuccess(CategoryEntity result) {
                if (onSuccess != null) {
                    appExecutors.mainThread().execute(onSuccess);
                }
            }
            @Override
            public void onError(Exception e) {}
        });
    }

    public void deleteCategory(int categoryId, RepositoryCallback<Void> callback) {
        menuApiService.getProducts(categoryId, null).enqueue(new Callback<List<ProductEntity>>() {
            @Override
            public void onResponse(Call<List<ProductEntity>> call, Response<List<ProductEntity>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int activeProductsCount = 0;
                    for (ProductEntity p : response.body()) {
                        if (p.isActive()) {
                            activeProductsCount++;
                        }
                    }
                    if (activeProductsCount > 0) {
                        Exception e = new Exception("Không thể xóa danh mục chứa sản phẩm đang hoạt động");
                        showError(e);
                        if (callback != null) {
                            appExecutors.mainThread().execute(() -> callback.onError(e));
                        }
                    } else {
                        menuApiService.deleteCategory(categoryId).enqueue(new Callback<Void>() {
                            @Override
                            public void onResponse(Call<Void> call, Response<Void> response) {
                                if (response.isSuccessful()) {
                                    appExecutors.mainThread().execute(() -> {
                                        // Xóa khỏi allCategoriesLive
                                        List<CategoryEntity> currentAll = allCategoriesLive.getValue();
                                        if (currentAll != null) {
                                            List<CategoryEntity> newAll = new java.util.ArrayList<>(currentAll);
                                            for (int i = 0; i < newAll.size(); i++) {
                                                if (newAll.get(i).getCategoryId() == categoryId) {
                                                    newAll.remove(i);
                                                    break;
                                                }
                                            }
                                            allCategoriesLive.setValue(newAll);
                                        }

                                        // Xóa khỏi activeCategoriesLive
                                        List<CategoryEntity> currentActive = activeCategoriesLive.getValue();
                                        if (currentActive != null) {
                                            List<CategoryEntity> newActive = new java.util.ArrayList<>(currentActive);
                                            for (int i = 0; i < newActive.size(); i++) {
                                                if (newActive.get(i).getCategoryId() == categoryId) {
                                                    newActive.remove(i);
                                                    break;
                                                }
                                            }
                                            activeCategoriesLive.setValue(newActive);
                                        }
                                    });
                                    if (callback != null) {
                                        appExecutors.mainThread().execute(() -> callback.onSuccess(null));
                                    }
                                } else {
                                    Exception e = parseError(response);
                                    showError(e);
                                    if (callback != null) {
                                        appExecutors.mainThread().execute(() -> callback.onError(e));
                                    }
                                }
                            }

                            @Override
                            public void onFailure(Call<Void> call, Throwable t) {
                                Exception e = new NetworkException("Không có kết nối mạng", t);
                                showError(e);
                                if (callback != null) {
                                    appExecutors.mainThread().execute(() -> callback.onError(e));
                                }
                            }
                        });
                    }
                } else {
                    Exception e = parseError(response);
                    showError(e);
                    if (callback != null) {
                        appExecutors.mainThread().execute(() -> callback.onError(e));
                    }
                }
            }

            @Override
            public void onFailure(Call<List<ProductEntity>> call, Throwable t) {
                Exception e = new NetworkException("Không có kết nối mạng", t);
                showError(e);
                if (callback != null) {
                    appExecutors.mainThread().execute(() -> callback.onError(e));
                }
            }
        });
    }

    @Deprecated
    public void deleteCategory(int categoryId, Runnable onSuccess, Runnable onError) {
        deleteCategory(categoryId, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (onSuccess != null) {
                    appExecutors.mainThread().execute(onSuccess);
                }
            }
            @Override
            public void onError(Exception e) {
                if (onError != null) {
                    appExecutors.mainThread().execute(onError);
                }
            }
        });
    }

    private Exception parseError(Response<?> response) {
        if (response == null) return new NetworkException("Không có kết nối mạng");
        switch (response.code()) {
            case 401: return new Exception("Phiên đăng nhập hết hạn (401)");
            case 403: return new Exception("Không có quyền thực hiện (403)");
            case 404: return new Exception("Không tìm thấy dữ liệu (404)");
            case 500: return new Exception("Lỗi máy chủ (500)");
            default:  return new Exception("Lỗi hệ thống: " + response.code());
        }
    }

    private void showError(final Exception e) {
        appExecutors.mainThread().execute(() ->
                Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show()
        );
    }
}