import axiosInstance from './axiosInstance';

/**
 * Uploads an image to the Spring Boot backend server.
 * The backend handles uploading it securely to Supabase Storage.
 * @param {File} file The file object to upload
 * @param {string} folder The subfolder inside the bucket (e.g. 'products', 'news')
 * @returns {Promise<string>} The public URL of the uploaded image
 */
export const uploadImage = async (file, folder = 'general') => {
  if (!file) throw new Error('File không hợp lệ');

  const formData = new FormData();
  formData.append('file', file);

  // POST multipart request to our Java server upload endpoint
  const res = await axiosInstance.post(`/api/v1/uploads?folder=${folder}`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });

  // Return the public URL returned by the backend
  return res.data.url;
};
