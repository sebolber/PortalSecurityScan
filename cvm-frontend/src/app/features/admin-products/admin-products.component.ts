import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import {
  ProductCreateRequest,
  ProductVersionCreateRequest,
  ProductVersionView,
  ProductView,
  ProductsService
} from '../../core/products/products.service';
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
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatTableModule,
    UuidChipComponent
  ],
  templateUrl: './admin-products.component.html',
  styleUrls: ['./admin-products.component.scss']
})
export class AdminProductsComponent implements OnInit {
  private readonly products = inject(ProductsService);
  private readonly snack = inject(MatSnackBar);

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

  readonly produktSpalten = ['uuid', 'key', 'name', 'description', 'aktion'];
  readonly versionSpalten = ['uuid', 'version', 'gitCommit', 'releasedAt', 'aktion'];

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
      this.snack.open('Versionen konnten nicht geladen werden.', 'OK',
        { duration: 3000 });
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
      this.snack.open('Key und Name sind Pflichtfelder.', 'OK',
        { duration: 3000 });
      return;
    }
    if (!/^[a-z0-9-]{2,64}$/.test(data.key.trim())) {
      this.snack.open(
        'Key muss kleingeschrieben und nur Ziffern/Bindestrich enthalten (2-64 Zeichen).',
        'OK',
        { duration: 4000 }
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
      this.snack.open(`Produkt "${erstellt.key}" angelegt.`, 'OK',
        { duration: 3000 });
      this.produktForm.set({ key: '', name: '', description: '' });
      form.resetForm({ key: '', name: '', description: '' });
      await this.ladeProdukte();
    } catch (err) {
      this.snack.open(this.fehlermeldung(err, 'Anlegen fehlgeschlagen.'),
        'OK', { duration: 5000 });
    } finally {
      this.produktPending.set(false);
    }
  }

  async legeVersionAn(form: NgForm): Promise<void> {
    const productId = this.selectedProductId();
    if (!productId) {
      this.snack.open('Bitte zuerst ein Produkt auswaehlen.', 'OK',
        { duration: 3000 });
      return;
    }
    const data = this.versionForm();
    if (!data.version.trim()) {
      this.snack.open('Versionsnummer ist Pflicht.', 'OK', { duration: 3000 });
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
      this.snack.open(`Version "${erstellt.version}" angelegt.`, 'OK',
        { duration: 3000 });
      this.versionForm.set({ version: '', gitCommit: '', releasedAt: '' });
      form.resetForm({ version: '', gitCommit: '', releasedAt: '' });
      const aktualisiert = await this.products.versions(productId);
      this.versionen.set({ ...this.versionen(), [productId]: aktualisiert });
    } catch (err) {
      this.snack.open(this.fehlermeldung(err, 'Versions-Anlage fehlgeschlagen.'),
        'OK', { duration: 5000 });
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
    const bestaetigt = window.confirm(
      `Version "${v.version}" wirklich soft-loeschen? Scans und Findings bleiben erhalten.`
    );
    if (!bestaetigt) {
      return;
    }
    try {
      await this.products.deleteVersion(productId, v.id);
      this.snack.open(`Version "${v.version}" geloescht.`, 'OK', { duration: 3000 });
      await this.waehleProdukt(productId);
    } catch (err) {
      this.snack.open(
        this.fehlermeldung(err, 'Loeschen der Version fehlgeschlagen.'),
        'OK', { duration: 5000 });
    }
  }

  /**
   * Soft-Delete mit Bestaetigungsdialog (Iteration 38, CVM-82).
   * Referenzierte Scans/Findings bleiben erhalten, das Produkt
   * verschwindet nur aus Admin-/Queue-Listen.
   */
  async loescheProdukt(p: ProductView): Promise<void> {
    const bestaetigt = window.confirm(
      `Produkt "${p.key}" wirklich soft-loeschen? Bestehende Scans bleiben erhalten.`
    );
    if (!bestaetigt) {
      return;
    }
    try {
      await this.products.delete(p.id);
      if (this.selectedProductId() === p.id) {
        this.selectedProductId.set(null);
      }
      this.snack.open(`Produkt "${p.key}" geloescht.`, 'OK', { duration: 3000 });
      await this.ladeProdukte();
    } catch (err) {
      this.snack.open(
        this.fehlermeldung(err, 'Loeschen fehlgeschlagen.'),
        'OK', { duration: 5000 });
    }
  }

  /**
   * Minimal-Edit fuer Name/Beschreibung eines Produkts. Nutzt zwei
   * window.prompt-Dialoge, damit der Admin-Flow ohne eigenes
   * Formular-Modal auskommt (Iteration 37, CVM-81).
   */
  async bearbeiteProdukt(p: ProductView): Promise<void> {
    const neuerName = window.prompt(
      `Name fuer Produkt "${p.key}"`, p.name);
    if (neuerName === null) {
      return;
    }
    const neueBeschreibung = window.prompt(
      `Beschreibung fuer Produkt "${p.key}" (leer lassen = keine)`,
      p.description ?? '');
    try {
      const aktualisiert = await this.products.update(p.id, {
        name: neuerName.trim(),
        description: neueBeschreibung === null ? null : neueBeschreibung.trim()
      });
      this.snack.open(
        `Produkt "${aktualisiert.key}" aktualisiert.`,
        'OK',
        { duration: 3000 }
      );
      await this.ladeProdukte();
    } catch (err) {
      this.snack.open(
        this.fehlermeldung(err, 'Aktualisierung fehlgeschlagen.'),
        'OK',
        { duration: 5000 }
      );
    }
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
