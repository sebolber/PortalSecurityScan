import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import {
  ProductVersionView,
  ProductView,
  ProductsService
} from '../../core/products/products.service';

@Component({
  selector: 'cvm-components',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatListModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './components.component.html',
  styleUrls: ['./components.component.scss']
})
export class ComponentsComponent implements OnInit {
  private readonly service = inject(ProductsService);

  readonly products = signal<readonly ProductView[]>([]);
  readonly versions = signal<readonly ProductVersionView[]>([]);
  readonly selectedProduct = signal<ProductView | null>(null);
  readonly loadingProducts = signal<boolean>(false);
  readonly loadingVersions = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    void this.ladeProdukte();
  }

  async ladeProdukte(): Promise<void> {
    this.loadingProducts.set(true);
    this.error.set(null);
    try {
      const list = await this.service.list();
      this.products.set(list);
      if (list.length > 0 && !this.selectedProduct()) {
        await this.waehleProdukt(list[0]);
      }
    } catch {
      this.error.set('Produkte konnten nicht geladen werden.');
    } finally {
      this.loadingProducts.set(false);
    }
  }

  async waehleProdukt(product: ProductView): Promise<void> {
    this.selectedProduct.set(product);
    this.loadingVersions.set(true);
    try {
      const vs = await this.service.versions(product.id);
      this.versions.set(vs);
    } catch {
      this.versions.set([]);
    } finally {
      this.loadingVersions.set(false);
    }
  }
}
