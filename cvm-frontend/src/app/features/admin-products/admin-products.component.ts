import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import {
  ProductCreateRequest,
  ProductVersionCreateRequest,
  ProductVersionView,
  ProductView,
  ProductsService
} from '../../core/products/products.service';
import { CvmConfirmService } from '../../shared/components/cvm-confirm.service';
import { CvmDialogComponent } from '../../shared/components/cvm-dialog.component';
import { CvmIconComponent } from '../../shared/components/cvm-icon.component';
import { CvmToastService } from '../../shared/components/cvm-toast.service';
import { UuidChipComponent } from '../../shared/components/uuid-chip.component';

interface ProductCreateForm {
  key: string;
  name: string;
  description: string;
}

interface VersionCreateForm {
  version: string;
  gitCommit: string;
  releasedAt: string;
}

/**
 * Admin-Seite zum Anlegen von Produkten und Versionen.
 * Listet bestehende Produkte, zeigt deren Versionen und bietet
 * zwei Formulare: Produkt-Anlage und Versions-Anlage.
 */
@Component({
  selector: 'cvm-admin-products',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    CvmIconComponent,
    CvmDialogComponent,
    UuidChipComponent
  ],
  templateUrl: './admin-products.component.html',
  styleUrls: ['./admin-products.component.scss']
})
export class AdminProductsComponent implements OnInit {
  private readonly products = inject(ProductsService);
  private readonly toast = inject(CvmToastService);
  private readonly confirmService = inject(CvmConfirmService);

  readonly produkte = signal<readonly ProductView[]>([]);
  readonly versionen = signal<Record<string, readonly ProductVersionView[]>>({});
  readonly laedt = signal<boolean>(false);
  readonly fehler = signal<string | null>(null);
  readonly selectedProductId = signal<string | null>(null);

  readonly produktForm = signal<ProductCreateForm>({
    key: '',
    name: '',
    description: ''
  });
  readonly produktPending = signal<boolean>(false);

  readonly versionForm = signal<VersionCreateForm>({
    version: '',
    gitCommit: '',
    releasedAt: ''
  });
  readonly versionPending = signal<boolean>(false);

  // Iteration 89 (CVM-329): Edit-Dialog ersetzt window.prompt-Kette.
  readonly editDialogOffen = signal(false);
  readonly editProdukt = signal<ProductView | null>(null);
  readonly editName = signal('');
  readonly editBeschreibung = signal('');
  readonly editRepoUrl = signal('');
  readonly editPending = signal(false);
  readonly editRepoUrlFehler = signal<string | null>(null);

  async ngOnInit(): Promise<void> {
    await this.ladeProdukte();
  }

  async ladeProdukte(): Promise<void> {
    this.laedt.set(true);
    this.fehler.set(null);
    try {
      const list = await this.products.list();
      this.produkte.set(list);
    } catch {
      this.fehler.set('Produkte konnten nicht geladen werden.');
    } finally {
      this.laedt.set(false);
    }
  }

  async waehleProdukt(id: string): Promise<void> {
    this.selectedProductId.set(id);
    try {
      const versions = await this.products.versions(id);
      this.versionen.set({ ...this.versionen(), [id]: versions });
    } catch {
      this.toast.error('Versionen konnten nicht geladen werden.');
    }
  }

  updateProduktForm(patch: Partial<ProductCreateForm>): void {
    this.produktForm.set({ ...this.produktForm(), ...patch });
  }

  updateVersionForm(patch: Partial<VersionCreateForm>): void {
    this.versionForm.set({ ...this.versionForm(), ...patch });
  }

  async legeProduktAn(form: NgForm): Promise<void> {
    const data = this.produktForm();
    if (!data.key.trim() || !data.name.trim()) {
      this.toast.warning('Key und Name sind Pflichtfelder.');
      return;
    }
    if (!/^[a-z0-9-]{2,64}$/.test(data.key.trim())) {
      this.toast.warning(
        'Key muss kleingeschrieben und nur Ziffern/Bindestrich enthalten (2-64 Zeichen).'
      );
      return;
    }
    this.produktPending.set(true);
    try {
      const req: ProductCreateRequest = {
        key: data.key.trim(),
        name: data.name.trim(),
        description: data.description.trim() || null
      };
      const erstellt = await this.products.create(req);
      this.toast.success(`Produkt "${erstellt.key}" angelegt.`, 3000);
      this.produktForm.set({ key: '', name: '', description: '' });
      form.resetForm({ key: '', name: '', description: '' });
      await this.ladeProdukte();
    } catch (err) {
      this.toast.error(this.fehlermeldung(err, 'Anlegen fehlgeschlagen.'));
    } finally {
      this.produktPending.set(false);
    }
  }

  async legeVersionAn(form: NgForm): Promise<void> {
    const productId = this.selectedProductId();
    if (!productId) {
      this.toast.warning('Bitte zuerst ein Produkt auswaehlen.');
      return;
    }
    const data = this.versionForm();
    if (!data.version.trim()) {
      this.toast.warning('Versionsnummer ist Pflicht.');
      return;
    }
    this.versionPending.set(true);
    try {
      const req: ProductVersionCreateRequest = {
        version: data.version.trim(),
        gitCommit: data.gitCommit.trim() || null,
        releasedAt: data.releasedAt ? new Date(data.releasedAt).toISOString() : null
      };
      const erstellt = await this.products.createVersion(productId, req);
      this.toast.success(`Version "${erstellt.version}" angelegt.`, 3000);
      this.versionForm.set({ version: '', gitCommit: '', releasedAt: '' });
      form.resetForm({ version: '', gitCommit: '', releasedAt: '' });
      const aktualisiert = await this.products.versions(productId);
      this.versionen.set({ ...this.versionen(), [productId]: aktualisiert });
    } catch (err) {
      this.toast.error(this.fehlermeldung(err, 'Versions-Anlage fehlgeschlagen.'));
    } finally {
      this.versionPending.set(false);
    }
  }

  versionenFuer(productId: string): readonly ProductVersionView[] {
    return this.versionen()[productId] ?? [];
  }

  /**
   * Soft-Delete einer Produkt-Version (Iteration 49, CVM-99).
   */
  async loescheVersion(productId: string, v: ProductVersionView): Promise<void> {
    const bestaetigt = await this.confirmService.confirm({
      title: 'Version entfernen',
      message: `Version "${v.version}" wirklich soft-loeschen?\n\n`
        + 'Scans und Findings bleiben erhalten.',
      confirmLabel: 'Entfernen',
      variant: 'danger'
    });
    if (!bestaetigt) {
      return;
    }
    try {
      await this.products.deleteVersion(productId, v.id);
      this.toast.success(`Version "${v.version}" geloescht.`, 3000);
      await this.waehleProdukt(productId);
    } catch (err) {
      this.toast.error(this.fehlermeldung(err, 'Loeschen der Version fehlgeschlagen.'));
    }
  }

  /**
   * Soft-Delete mit Bestaetigungsdialog (Iteration 38, CVM-82).
   * Referenzierte Scans/Findings bleiben erhalten, das Produkt
   * verschwindet nur aus Admin-/Queue-Listen.
   */
  async loescheProdukt(p: ProductView): Promise<void> {
    const bestaetigt = await this.confirmService.confirm({
      title: 'Produkt entfernen',
      message: `Produkt "${p.key}" wirklich soft-loeschen?\n\n`
        + 'Bestehende Scans bleiben erhalten.',
      confirmLabel: 'Entfernen',
      variant: 'danger'
    });
    if (!bestaetigt) {
      return;
    }
    try {
      await this.products.delete(p.id);
      if (this.selectedProductId() === p.id) {
        this.selectedProductId.set(null);
      }
      this.toast.success(`Produkt "${p.key}" geloescht.`, 3000);
      await this.ladeProdukte();
    } catch (err) {
      this.toast.error(this.fehlermeldung(err, 'Loeschen fehlgeschlagen.'));
    }
  }

  /**
   * Minimal-Edit fuer Name/Beschreibung eines Produkts. Nutzt zwei
   * window.prompt-Dialoge, damit der Admin-Flow ohne eigenes
   * Formular-Modal auskommt (Iteration 37, CVM-81).
   */
  /**
   * Iteration 89 (CVM-329): ersetzt die bisherige window.prompt-
   * Kette durch einen cvm-dialog mit echten Formular-Feldern.
   */
  bearbeiteProdukt(p: ProductView): void {
    this.editProdukt.set(p);
    this.editName.set(p.name);
    this.editBeschreibung.set(p.description ?? '');
    this.editRepoUrl.set(p.repoUrl ?? '');
    this.editRepoUrlFehler.set(null);
    this.editDialogOffen.set(true);
  }

  async speichereEdit(): Promise<void> {
    const p = this.editProdukt();
    if (!p) {
      return;
    }
    const name = this.editName().trim();
    if (!name) {
      this.toast.warning('Name ist Pflichtfeld.');
      return;
    }
    const repoUrl = this.editRepoUrl().trim();
    if (repoUrl && !/^(https?:\/\/|git@|ssh:\/\/)/.test(repoUrl)) {
      this.editRepoUrlFehler.set(
        'URL muss mit http(s)://, ssh:// oder git@ beginnen.'
      );
      return;
    }
    this.editRepoUrlFehler.set(null);
    this.editPending.set(true);
    try {
      const aktualisiert = await this.products.update(p.id, {
        name,
        description: this.editBeschreibung().trim(),
        repoUrl
      });
      this.toast.success(`Produkt "${aktualisiert.key}" aktualisiert.`, 3000);
      this.editDialogOffen.set(false);
      this.editProdukt.set(null);
      await this.ladeProdukte();
    } catch (err) {
      this.toast.error(this.fehlermeldung(err, 'Aktualisierung fehlgeschlagen.'));
    } finally {
      this.editPending.set(false);
    }
  }

  brecheEditAb(): void {
    this.editDialogOffen.set(false);
    this.editProdukt.set(null);
    this.editRepoUrlFehler.set(null);
  }

  private fehlermeldung(err: unknown, fallback: string): string {
    if (err && typeof err === 'object') {
      const obj = err as { status?: number; error?: { error?: string; message?: string } };
      if (obj.status === 409 && obj.error?.error === 'product_key_conflict') {
        return 'Produkt-Key ist bereits vergeben.';
      }
      if (obj.status === 409 && obj.error?.error === 'product_version_conflict') {
        return 'Version existiert bereits fuer dieses Produkt.';
      }
      if (obj.status === 404) {
        return 'Produkt nicht gefunden.';
      }
      if (obj.status === 403) {
        return 'Keine Berechtigung. CVM_ADMIN erforderlich.';
      }
      if (obj.status === 400 && obj.error?.message) {
        return obj.error.message;
      }
    }
    return fallback;
  }
}
