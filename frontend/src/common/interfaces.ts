export interface IdSupplier {
  id: any;
}

export interface InvoiceDTO extends IdSupplier {
  id: number;
  userId: number;
  userFullName: string;
  dateIssued: Date;
  dueDate: Date;
  amount: number;
  status: string;
  lessons?: LessonDTO[];
}

export interface LessonDTO {
  id: number;
  topic: string;
  notes: string;
  date: string;
  groupId: number;
}
