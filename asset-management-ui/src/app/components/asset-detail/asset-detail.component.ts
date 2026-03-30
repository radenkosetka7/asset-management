import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { AssetService } from '../../services/asset.service';
import { AssetResponse, AssetStatus, AssetAttributeValueResponse, ImageResponse } from '../../models/asset.model';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-asset-detail',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './asset-detail.component.html',
  styleUrl: './asset-detail.component.css'
})
export class AssetDetailComponent implements OnInit {
  asset = signal<AssetResponse | null>(null);
  loading = signal(true);
  error = signal<string | null>(null);
  selectedImageIndex = signal(0);
  brokenImageIds = signal<Set<number>>(new Set<number>());

  deletingImage = signal<{ assetId: number; imageId: number; fileName: string } | null>(null);
  deletingImageBusy = signal(false);

  deletingAttr = signal<{ assetId: number; attributeId: number; attrName: string } | null>(null);
  deletingAttrBusy = signal(false);

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private assetService: AssetService,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.error.set('Invalid asset ID.');
      this.loading.set(false);
      return;
    }
    this.loadAsset(id);
  }

  private loadAsset(id: number): void {
    this.loading.set(true);
    this.assetService.getById(id).subscribe({
      next: (assetData: AssetResponse) => {
        this.asset.set(assetData);
        const imgs = assetData.images ?? [];
        if (this.selectedImageIndex() >= imgs.length) this.selectedImageIndex.set(0);
        // Keep only broken IDs that still exist after reload.
        const validImageIds = new Set(imgs.map((img) => img.id));
        this.brokenImageIds.set(new Set([...this.brokenImageIds()].filter((id) => validImageIds.has(id))));
        this.loading.set(false);
      },
      error: (_err: unknown) => {
        console.error(_err);
        this.error.set('Failed to load asset details.');
        this.loading.set(false);
      }
    });
  }

  confirmDeleteImage(img: ImageResponse): void {
    const a = this.asset();
    if (!a) return;
    this.deletingImage.set({ assetId: a.id, imageId: img.id, fileName: img.fileName });
  }
  cancelDeleteImage(): void { this.deletingImage.set(null); }

  deleteImage(): void {
    const d = this.deletingImage();
    if (!d) return;
    this.deletingImageBusy.set(true);
    this.assetService.deleteAssetImage(d.assetId, d.imageId).subscribe({
      next: () => {
        this.deletingImage.set(null);
        this.deletingImageBusy.set(false);
        this.loadAsset(d.assetId);
      },
      error: () => {
        this.deletingImageBusy.set(false);
        this.error.set('Failed to delete image.');
        this.deletingImage.set(null);
      }
    });
  }

  confirmDeleteAttr(attr: AssetAttributeValueResponse): void {
    const a = this.asset();
    if (!a) return;
    this.deletingAttr.set({ assetId: a.id, attributeId: attr.attribute.id, attrName: attr.attribute.name });
  }
  cancelDeleteAttr(): void { this.deletingAttr.set(null); }

  deleteAttr(): void {
    const d = this.deletingAttr();
    if (!d) return;
    this.deletingAttrBusy.set(true);
    this.assetService.deleteAssetAttribute(d.assetId, d.attributeId).subscribe({
      next: () => {
        this.deletingAttr.set(null);
        this.deletingAttrBusy.set(false);
        this.loadAsset(d.assetId);
      },
      error: () => {
        this.deletingAttrBusy.set(false);
        this.error.set('Failed to delete attribute value.');
        this.deletingAttr.set(null);
      }
    });
  }

  get statusClass(): string {
    const a = this.asset();
    if (!a) return '';
    const map: Record<AssetStatus, string> = {
      ACTIVE: 'status-active',
      IN_USE: 'status-in-use',
      DAMAGED: 'status-damaged',
      RETIRED: 'status-retired'
    };
    return map[a.assetStatus] ?? 'status-active';
  }

  get statusLabel(): string {
    const a = this.asset();
    if (!a) return '';
    const map: Record<AssetStatus, string> = {
      ACTIVE: 'Active',
      IN_USE: 'In Use',
      DAMAGED: 'Damaged',
      RETIRED: 'Retired'
    };
    return map[a.assetStatus] ?? a.assetStatus;
  }

  imageUrl(fileName: string): string {
    return '/api/assets/images/file/' + encodeURIComponent(fileName);
  }

  selectedImage(): ImageResponse | null {
    const images = this.asset()?.images ?? [];
    return images[this.selectedImageIndex()] ?? null;
  }

  isImageBroken(imageId: number): boolean {
    return this.brokenImageIds().has(imageId);
  }

  markImageBroken(imageId: number): void {
    const current = this.brokenImageIds();
    if (current.has(imageId)) return;
    this.brokenImageIds.set(new Set([...current, imageId]));
  }

  getAttributeValue(attr: AssetAttributeValueResponse): string {
    if (attr.valueString !== null && attr.valueString !== undefined) return attr.valueString;
    if (attr.valueNumber !== null && attr.valueNumber !== undefined) return String(attr.valueNumber);
    if (attr.valueDate !== null && attr.valueDate !== undefined) return attr.valueDate;
    if (attr.valueBool !== null && attr.valueBool !== undefined) return attr.valueBool ? 'Yes' : 'No';
    return '—';
  }

  get isAdminContext(): boolean {
    return this.router.url.startsWith('/admin');
  }

  goBack(): void {
    if (this.isAdminContext) this.router.navigate(['/admin/assets']);
    else this.router.navigate(['/']);
  }
}
