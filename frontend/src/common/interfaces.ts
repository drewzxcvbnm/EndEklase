
export interface IdSupplier {
  id: any;
}


export interface InvoiceDTO {
  id: number;
  userId: number;
  userFullName: string;
  dateIssued: Date;
  dueDate: Date;
  amount: number;
  status: string;
}


