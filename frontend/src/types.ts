export type ApiResponse<T> = { success: boolean; code: string; message: string; data: T }
export type User = { userId: number; phone: string; nickname: string; role: 'USER' | 'MERCHANT' }
export type Brand = { id: number; name: string; description?: string }
export type Store = {
  id: number; name: string; category: string; address: string; rating: number | null;
  longitude?: number; latitude?: number; distance?: number; brandId?: number; brandName?: string;
  status: 'ACTIVE' | 'CLOSED'
}
export type Promotion = {
  id: number; businessId: number; businessName: string; title: string; stock: number;
  beginTime: string; endTime: string; orderCount: number; status: string
}
export type Page<T> = { content: T[] }
export type MyVoucher = {
  voucherId: number; voucherTitle: string; storeId: number; storeName: string;
  startTime: string; endTime: string; orderId: number; status: 'AVAILABLE' | 'EXPIRED'
}
