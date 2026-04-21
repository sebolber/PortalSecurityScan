import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CvmIconComponent } from '../../shared/components/cvm-icon.component';
import { CvmToastService } from '../../shared/components/cvm-toast.service';
import {
  EnvironmentView,
  EnvironmentsService
} from '../../core/environments/environments.service';
import {
  ProductVersionView,
  ProductView,
  ProductsService
} from '../../core/products/products.service';
import {
  ScanSummary,
  ScansService
} from '../../core/scans/scans.service';

const MAX_SIZE_BYTES = 30 * 1024 * 1024;

type UploadState = 'idle' | 'uploading' | 'polling' | 'done' | 'error';

/**
 * Upload einer CycloneDX-SBOM fuer eine konkrete Produktversion.
 * Die UI zeigt Produkt- und Versionsauswahl, eine Dropzone und pollt
 * nach erfolgreichem Upload den Scan-Status.
 *
 * Iteration 61 (CVM-62): Migration von Angular Material auf pure
 * Tailwind-Komponenten, MatSnackBar ersetzt durch CvmToastService.
 */
@Component({
  selector: 'cvm-scan-upload',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    CvmIconComponent
  ],
  templateUrl: './scan-upload.component.html',
  styleUrls: ['./scan-upload.component.scss']
})
export class ScanUploadComponent implements OnInit {
  private readonly products = inject(ProductsService);
  private readonly envService = inject(EnvironmentsService);
  private readonly scans = inject(ScansService);
  private readonly toast = inject(CvmToastService);

  readonly produkte = signal<readonly ProductView[]>([]);
  readonly versionen = signal<readonly ProductVersionView[]>([]);
  readonly umgebungen = signal<readonly EnvironmentView[]>([]);
  readonly selectedProductId = signal<string | null>(null);
  readonly selectedVersionId = signal<string | null>(null);
  readonly selectedEnvironmentId = signal<string | null>(null);
  readonly selectedFile = signal<File | null>(null);
  readonly dragOver = signal<boolean>(false);
  readonly state = signal<UploadState>('idle');
  readonly fehler = signal<string | null>(null);
  readonly summary = signal<ScanSummary | null>(null);

  /**
   * Iteration 80 (CVM-320): Deep-Link in die Bewertungs-Queue,
   * gefiltert auf die Produkt-Version + Umgebung des gerade
   * hochgeladenen Scans.
   */
  readonly queueLinkParams = computed(() => {
    const versionId = this.selectedVersionId();
    const environmentId = this.selectedEnvironmentId();
    if (!versionId) {
      return null;
    }
    const params: Record<string, string> = {
      productVersionId: versionId
    };
    if (environmentId) {
      params['environmentId'] = environmentId;
    }
    return params;
  });

  async ngOnInit(): Promise<void> {
    try {
      const [produkte, umgebungen] = await Promise.all([
        this.products.list(),
        this.envService.list()
      ]);
      this.produkte.set(produkte);
      this.umgebungen.set(umgebungen);
    } catch {
      this.fehler.set('Stammdaten konnten nicht geladen werden.');
    }
  }

  async onProductChange(productId: string): Promise<void> {
    this.selectedProductId.set(productId);
    this.selectedVersionId.set(null);
    this.versionen.set([]);
    if (!productId) {
      return;
    }
    try {
      const versions = await this.products.versions(productId);
      this.versionen.set(versions);
    } catch {
      this.toast.error('Versionen konnten nicht geladen werden.');
    }
  }

  onVersionChange(versionId: string): void {
    this.selectedVersionId.set(versionId);
  }

  onEnvironmentChange(envId: string): void {
    this.selectedEnvironmentId.set(envId || null);
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(true);
  }

  onDragLeave(): void {
    this.dragOver.set(false);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(false);
    const file = event.dataTransfer?.files?.[0];
    if (file) {
      this.setzeDatei(file);
    }
  }

  onFileInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) {
      this.setzeDatei(file);
    }
  }

  private setzeDatei(file: File): void {
    if (file.size > MAX_SIZE_BYTES) {
      this.toast.error(
        `Datei zu gross (${(file.size / 1024 / 1024).toFixed(1)} MB, max 30 MB).`
      );
      return;
    }
    const name = file.name.toLowerCase();
    if (!name.endsWith('.json') && file.type !== 'application/json') {
      this.toast.error(
        'Nur CycloneDX-JSON-Dateien (.json) werden unterstuetzt.'
      );
      return;
    }
    this.selectedFile.set(file);
    this.summary.set(null);
    this.fehler.set(null);
    this.state.set('idle');
  }

  async hochladen(): Promise<void> {
    const versionId = this.selectedVersionId();
    const environmentId = this.selectedEnvironmentId();
    const file = this.selectedFile();
    if (!versionId || !environmentId || !file) {
      this.toast.warning('Bitte Version, Umgebung und Datei auswaehlen.');
      return;
    }
    this.state.set('uploading');
    this.fehler.set(null);
    this.summary.set(null);
    try {
      const response = await this.scans.uploadSbom(
        {
          productVersionId: versionId,
          environmentId: this.selectedEnvironmentId(),
          scanner: 'trivy'
        },
        file);
      this.state.set('polling');
      await this.pollStatus(response.scanId);
    } catch (err) {
      this.state.set('error');
      this.fehler.set(this.formatError(err));
    }
  }

  private async pollStatus(scanId: string): Promise<void> {
    const maxAttempts = 30;
    for (let attempt = 0; attempt < maxAttempts; attempt++) {
      try {
        const summary = await this.scans.getStatus(scanId);
        this.summary.set(summary);
        if (summary.componentCount > 0 || summary.findingCount > 0) {
          this.state.set('done');
          this.toast.success('Scan verarbeitet.', 3000);
          return;
        }
      } catch {
        // Noch nicht verfuegbar, weiter pollen.
      }
      await new Promise((r) => setTimeout(r, 2000));
    }
    this.state.set('done');
    this.toast.info(
      'Scan wurde akzeptiert. Details werden asynchron verarbeitet.',
      4000
    );
  }

  private formatError(err: unknown): string {
    if (err && typeof err === 'object') {
      const obj = err as { status?: number;
        error?: { error?: string; message?: string } };
      if (obj.status === 409 && obj.error?.error === 'scan_already_ingested') {
        return 'Diese SBOM wurde bereits hochgeladen (SHA-256 identisch).';
      }
      if (obj.status === 400 && obj.error?.error === 'sbom_schema_error') {
        return 'SBOM ist kein gueltiges CycloneDX: ' +
          (obj.error?.message ?? '');
      }
      if (obj.status === 400 && obj.error?.error === 'sbom_parse_error') {
        return 'SBOM konnte nicht geparst werden (kein gueltiges JSON).';
      }
      if (obj.status === 400 && obj.error?.error === 'environment_required') {
        return 'Umgebung ist Pflicht - bitte im Formular auswaehlen.';
      }
      if (obj.status === 413) {
        return 'Datei zu gross fuer den Server.';
      }
      if (obj.status === 403) {
        return 'Keine Berechtigung. CVM_ADMIN oder CVM_ASSESSOR erforderlich.';
      }
    }
    return 'Upload fehlgeschlagen.';
  }
}
