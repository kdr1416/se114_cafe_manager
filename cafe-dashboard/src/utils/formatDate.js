export const formatEpoch = (epoch, pattern = 'dd/MM/yyyy') => {
  if (!epoch) return '--';
  const date = new Date(epoch);
  return new Intl.DateTimeFormat('vi-VN', {
    timeZone: 'Asia/Ho_Chi_Minh',
    day: '2-digit', month: '2-digit', year: 'numeric'
  }).format(date);
};

export const formatEpochTime = (epoch) => {
  if (!epoch) return '--';
  return new Intl.DateTimeFormat('vi-VN', {
    timeZone: 'Asia/Ho_Chi_Minh',
    hour: '2-digit', minute: '2-digit'
  }).format(new Date(epoch));
};

export const formatEpochFull = (epoch) => {
  if (!epoch) return '--';
  return new Intl.DateTimeFormat('vi-VN', {
    timeZone: 'Asia/Ho_Chi_Minh',
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit'
  }).format(new Date(epoch));
};
