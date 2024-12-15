import React, { useState, useEffect } from "react";
import { useSelector } from "react-redux";
import { getRequest, postRequest, putRequest } from "src/api";
import { RootState } from "src/redux/store";
import InvoiceForm from "./InvoiceForm";
import GenerateInvoicesForm from "./GenerateInvoicesForm";
import InvoiceList from "./InvoiceList";
import CrudTable from "./common/CrudTable";
import RootObjectForm from "./common/RootObjectForm";
import TextInput from "./common/TextInput";
import BooleanInput from "./common/BooleanInput";
import NumberInput from "./common/NumberInput";
import { InvoiceDTO } from "src/common/interfaces";

const InvoicesTab: React.FC = () => {
  const [usersInfo, setUsersInfo] = useState<{ id: number; fullName: string }[]>([]);
  const [lessons, setLessons] = useState<any[]>([]);
  const [groups, setGroups] = useState<any[]>([]);
  const [kindergartens, setKindergartens] = useState<any[]>([]);
  const [invoices, setInvoices] = useState<InvoiceDTO[]>([]);
  const [filters, setFilters] = useState<any>({
    fullName: "",
    dateIssuedFrom: "",
    dateIssuedTo: "",
    dueDateFrom: "",
    dueDateTo: "",
    status: "",
  });
  const [editingInvoiceId, setEditingInvoiceId] = useState<number | null>(null);
  const [editingInvoice, setEditingInvoice] = useState<any>(null);

  const token = useSelector((state: RootState) => state.auth.token);

  useEffect(() => {
    getRequest<{ id: number; fullName: string }[]>("admin/user-emails").then(setUsersInfo);
    loadLessons();
    loadKindergartens();
    loadInvoices();
  }, []);

  const loadLessons = async () => {
    if (token) {
      const fetchedLessons = await getRequest<any[]>("admin/lessons");
      setLessons(fetchedLessons || []);
    }
  };

  const loadKindergartens = async () => {
    if (token) {
      const fetchedKindergartens = await getRequest<any[]>("admin/kindergartens");
      setKindergartens(fetchedKindergartens || []);
    }
  };

  const loadInvoices = async () => {
    if (token) {
      const fetchedInvoices = await getRequest<any[]>("admin/invoices");
      setInvoices(fetchedInvoices || []);
    }
  };

  const handleFilterChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    setFilters({ ...filters, [e.target.name]: e.target.value });
  };

  const handleEdit = (invoice: any) => {
    setEditingInvoiceId(invoice.id);
    setEditingInvoice({ ...invoice });
  };

  const handleSave = async () => {
    if (editingInvoiceId && editingInvoice) {
      await putRequest(`admin/invoice`, editingInvoice);
      alert("Изменения сохранены");
      setEditingInvoiceId(null);
      setEditingInvoice(null);
      loadInvoices();
    }
  };

  const handleCancel = () => {
    setEditingInvoiceId(null);
    setEditingInvoice(null);
  };

  const handleInvoiceSave = (invoice: any) => {
    postRequest("admin/invoice", invoice).then(() => {
      loadInvoices();
    });
  };

  const handleGenerateInvoices = (from: string, to: string, groupId: number | "") => {
    // Логика генерации счетов
    postRequest("admin/generate-invoices", { from, to, groupId }).then(() => {
      loadInvoices();
    });
  };

  return (
    <div>
      <h2 className="text-3xl">Выставление счетов</h2>
      <div className="d-flex">
        <InvoiceForm usersInfo={usersInfo} lessons={lessons} onSave={handleInvoiceSave} />
        <GenerateInvoicesForm kindergartens={kindergartens} onGenerate={handleGenerateInvoices} />
      </div>
      <CrudTable
        items={invoices}
        onDelete={it => console.log(`Deleted ${JSON.stringify(it)}`)}
        editFormSupplier={(it, close) => {
          const [item, setItem] = useState(it);
          useEffect(() => setItem(it), [it]);
          return (
            <>
              <RootObjectForm rootObject={item} rootObjectSetter={setItem}>
                <NumberInput field="amount" header="Сумма" />
                <button onClick={() => {
                  it.amount = item.amount;
                  console.log("save");
                  close();
                }} className="btn btn-primary mt-3">
                  Сохранить
                </button>
              </RootObjectForm>
            </>
          );
        }}
      />
      {/*
      <InvoiceList
        invoices={invoices}
        filters={filters}
        onFilterChange={handleFilterChange}
        onEdit={handleEdit}
        onSave={handleSave}
        onCancel={handleCancel}
        editingInvoiceId={editingInvoiceId}
        editingInvoice={editingInvoice}
        setEditingInvoice={setEditingInvoice}
      />
      */}
    </div>
  );
};

export default InvoicesTab;
