import React, { useState, useEffect } from "react";
import { useSelector } from "react-redux";
import { getRequest, postRequest, putRequest, deleteInvoice, getLessonsByUser } from "src/api";
import { RootState } from "src/redux/store";
import InvoiceForm from "./InvoiceForm";
import GenerateInvoicesForm from "./GenerateInvoicesForm";
import CrudTable from "./common/CrudTable";
import RootObjectForm from "./common/RootObjectForm";
import NumberInput from "./common/NumberInput";
import {
  InvoiceDTO,
  InvoiceEditDTO,
  LessonDTO,
} from "src/common/interfaces";
import MultiSelect from "./common/MultiSelect";

const InvoicesTab: React.FC = () => {
  const [usersInfo, setUsersInfo] = useState<{ id: number; fullName: string }[]>([]);
  const [lessons, setLessons] = useState<LessonDTO[]>([]);
  const [invoices, setInvoices] = useState<InvoiceDTO[]>([]);
  const [userLessons, setUserLessons] = useState<LessonDTO[]>([]);

  const token = useSelector((state: RootState) => state.auth.token);

  useEffect(() => {
    getRequest<{ id: number; fullName: string }[]>("admin/user-emails").then(setUsersInfo);
    loadLessons();
    loadInvoices();
  }, []);

  const loadLessons = async () => {
    if (token) {
      const fetchedLessons = await getRequest<LessonDTO[]>("admin/lessons");
      setLessons(fetchedLessons || []);
    }
  };

  const loadInvoices = async () => {
    if (token) {
      const fetchedInvoices = await getRequest<InvoiceDTO[]>("admin/invoices");
  
      setInvoices((prevInvoices) => {
        const invoiceMap = new Map(prevInvoices.map(inv => [inv.id, inv]));
        return fetchedInvoices
          .map(inv => invoiceMap.get(inv.id) || inv)
          .sort((a, b) => a.id - b.id);
      });
    }
  };
  

  const handleDeleteInvoice = async (invoice: InvoiceDTO) => {
    if (!token || invoice.id === undefined) return;
    if (window.confirm(`Вы уверены, что хотите удалить инвойс №${invoice.id}?`)) {
      await deleteInvoice(token, invoice.id);
      setInvoices((prev) => prev.filter((i) => i.id !== invoice.id));
    }
  };

  const handleInvoiceSave = async (invoice: InvoiceDTO) => {
    try {
      const savedInvoice = await putRequest<InvoiceDTO>("admin/invoice", invoice);
  
      setInvoices((prevInvoices) =>
        prevInvoices.map((inv) => (inv.id === savedInvoice.id ? savedInvoice : inv))
      );
    } catch (error) {
      console.error("Ошибка при сохранении инвойса:", error);
      alert("Ошибка при сохранении инвойса");
    }
  };

  const handleGenerateInvoices = async (data: { startDate: string; endDate: string }) => {
    try {
      await postRequest("admin/invoices", data);
      await loadInvoices();
      setInvoices((prevInvoices) => 
        [...prevInvoices, ...generatedInvoices].sort((a, b) => a.id - b.id)
      );
  
      alert("Invoices generated successfully");
    } catch (error) {
      console.error("Ошибка при генерации инвойсов:", error);
      alert("Ошибка при генерации инвойсов");
    }
  };
  


  return (
    <div>
      <h2 className="text-3xl">Выставление счетов</h2>
      <div className="d-flex">
        <InvoiceForm usersInfo={usersInfo} lessons={lessons} onSave={handleInvoiceSave} />
        <GenerateInvoicesForm kindergartens={[]} onGenerate={handleGenerateInvoices} />
      </div>
      <CrudTable
        items={invoices as Required<InvoiceDTO>[]}
        onDelete={(item) => {
          const invoice = invoices.find((inv) => inv.id === item.id);
          if (invoice) {
            handleDeleteInvoice(invoice);
          }
        }}
        editFormSupplier={(it, close) => {
          const [item, setItem] = useState<InvoiceEditDTO>({
            ...it,
            lessons: Array.isArray(it.lessons) ? it.lessons : [],
          });

          const [userLessons, setUserLessons] = useState<LessonDTO[]>([]);

          useEffect(() => {
            if (token && it.userId) {
              getLessonsByUser(token, it.userId).then(setUserLessons); 
            }
          }, [it.userId, token]);
        
          useEffect(() => {
            if (userLessons.length > 0) {
              setItem((prev) => ({
                ...prev,
                lessons: userLessons,
              }));
            }
          }, [userLessons]);

          return (
            <RootObjectForm rootObject={item} rootObjectSetter={setItem}>
              <NumberInput field="amount" header="Сумма" />
              <MultiSelect
                field="lessons"
                columns={["id", "topic", "date"]}
                columnMap={{
                  id: "ID Урока",
                  topic: "Тема",
                  date: "Дата",
                }}
                options={userLessons.map((i) => ({
                  ...i,
                  name: `${i.topic} (${i.date})`,
                }))}
              />
              <div className="mt-3 flex justify-end space-x-2">
                <button
                  onClick={() => {
                    putRequest("admin/invoice", item).then(() => {
                      setInvoices((prev) =>
                        prev.map((inv) => (inv.id === item.id ? item : inv))
                      );
                    });
                    close();
                  }}
                  className="btn btn-primary"
                >
                  Сохранить
                </button>
                <button onClick={close} className="btn btn-secondary">
                  Закрыть
                </button>
              </div>
            </RootObjectForm>
          );
        }}
      />
    </div>
  );
};

export default InvoicesTab;
