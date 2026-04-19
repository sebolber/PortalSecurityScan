import { ChangeDetectionStrategy, Component, Input, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  LucideAngularModule,
  // Icons (Mapping nach Iteration 61-Plan, Abschnitt 7)
  AlertCircle,
  AlertTriangle,
  ArrowLeft,
  ArrowRight,
  BarChart3,
  Bell,
  Boxes,
  Check,
  CheckCircle2,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  ChevronUp,
  Circle,
  CircleCheck,
  CircleUser,
  ClipboardList,
  Clock,
  Copy,
  Database,
  Download,
  ExternalLink,
  Eye,
  EyeOff,
  FileText,
  Filter,
  Flag,
  Folder,
  GitBranch,
  Globe,
  HardDrive,
  Info,
  Inbox,
  Key,
  Layers,
  LayoutDashboard,
  ListChecks,
  Loader2,
  LogIn,
  LogOut,
  Mail,
  Menu,
  MessageSquare,
  Minus,
  Moon,
  MoreHorizontal,
  MoreVertical,
  Package,
  Pause,
  Pencil,
  Play,
  Plus,
  RefreshCw,
  Save,
  Search,
  Settings,
  Shield,
  ShieldAlert,
  ShieldCheck,
  Slash,
  Sparkles,
  Square,
  Sun,
  Tag,
  Trash2,
  TrendingUp,
  Upload,
  Users,
  Wrench,
  X,
  XCircle,
  Zap
} from 'lucide-angular';

/**
 * Iteration 61A (CVM-62): Einheitliches Icon-System auf Basis von
 * `lucide-angular`. Ersetzt Ligatur-Font `material-icons`.
 *
 * Verwendung:
 *   <cvm-icon name="search" />
 *   <cvm-icon name="check" [size]="20" />
 *
 * Der Name entspricht dem Lucide-Namen in kebab-case oder camelCase,
 * oder dem frueheren Material-Namen (siehe Mapping in der plan-Datei).
 */
const ICON_REGISTRY: Record<string, unknown> = {
  // Material -> Lucide Mapping (kebab-case)
  search: Search,
  close: X,
  check: Check,
  'check-circle': CheckCircle2,
  'check_circle': CheckCircle2,
  warning: AlertTriangle,
  error: AlertCircle,
  report: AlertCircle,
  info: Info,
  'arrow-drop-down': ChevronDown,
  'arrow_drop_down': ChevronDown,
  'expand-more': ChevronDown,
  'expand_more': ChevronDown,
  'expand-less': ChevronUp,
  'expand_less': ChevronUp,
  'chevron-left': ChevronLeft,
  'chevron-right': ChevronRight,
  'chevron-down': ChevronDown,
  'chevron-up': ChevronUp,
  'arrow-left': ArrowLeft,
  'arrow-right': ArrowRight,
  add: Plus,
  plus: Plus,
  remove: Minus,
  minus: Minus,
  delete: Trash2,
  trash: Trash2,
  edit: Pencil,
  pencil: Pencil,
  save: Save,
  upload: Upload,
  download: Download,
  'filter-list': Filter,
  'filter_list': Filter,
  filter: Filter,
  refresh: RefreshCw,
  'more-vert': MoreVertical,
  'more_vert': MoreVertical,
  'more-horiz': MoreHorizontal,
  'more_horiz': MoreHorizontal,
  settings: Settings,
  'account-circle': CircleUser,
  'account_circle': CircleUser,
  logout: LogOut,
  login: LogIn,
  layers: Layers,
  shield: Shield,
  'shield-check': ShieldCheck,
  'shield-alert': ShieldAlert,
  'light-mode': Sun,
  'light_mode': Sun,
  sun: Sun,
  'dark-mode': Moon,
  'dark_mode': Moon,
  moon: Moon,
  visibility: Eye,
  'visibility-off': EyeOff,
  'visibility_off': EyeOff,
  'content-copy': Copy,
  'content_copy': Copy,
  'open-in-new': ExternalLink,
  'open_in_new': ExternalLink,
  'play-arrow': Play,
  'play_arrow': Play,
  play: Play,
  stop: Square,
  pause: Pause,
  'radio-button-unchecked': Circle,
  'radio_button_unchecked': Circle,
  circle: Circle,
  clear: X,
  x: X,
  menu: Menu,
  inbox: Inbox,
  construction: Wrench,
  wrench: Wrench,
  bell: Bell,
  users: Users,
  package: Package,
  boxes: Boxes,
  folder: Folder,
  'bar-chart': BarChart3,
  'bar-chart-3': BarChart3,
  dashboard: LayoutDashboard,
  'layout-dashboard': LayoutDashboard,
  'clipboard-list': ClipboardList,
  'list-checks': ListChecks,
  'file-text': FileText,
  flag: Flag,
  mail: Mail,
  clock: Clock,
  key: Key,
  database: Database,
  'hard-drive': HardDrive,
  'git-branch': GitBranch,
  globe: Globe,
  'trending-up': TrendingUp,
  'message-square': MessageSquare,
  tag: Tag,
  'circle-check': CircleCheck,
  'circle-user': CircleUser,
  slash: Slash,
  sparkles: Sparkles,
  'x-circle': XCircle,
  zap: Zap,
  loader: Loader2,
  'loader-2': Loader2
};

@Component({
  selector: 'cvm-icon',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (resolved(); as r) {
      <lucide-icon
        [img]="r"
        [size]="sizePx()"
        [strokeWidth]="strokeWidth"
        [attr.aria-hidden]="ariaHidden ? 'true' : null"
      ></lucide-icon>
    } @else {
      <span class="cvm-icon-missing" aria-hidden="true">•</span>
    }
  `,
  styles: [
    `
      :host {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        line-height: 0;
      }
      .cvm-icon-missing {
        font-size: 14px;
        color: var(--color-text-muted);
      }
    `
  ]
})
export class CvmIconComponent {
  private readonly _name = signal('');

  @Input({ required: true })
  set name(value: string) {
    this._name.set((value ?? '').trim());
  }
  get name(): string {
    return this._name();
  }

  @Input() size: number | string = 20;
  @Input() strokeWidth = 2;
  @Input() ariaHidden = true;

  readonly sizePx = computed(() => {
    const s = this.size;
    return typeof s === 'number' ? s : parseInt(s, 10) || 20;
  });

  readonly resolved = computed(() => {
    const key = this._name();
    if (!key) return null;
    const normal = key.replace(/_/g, '-').toLowerCase();
    return (ICON_REGISTRY[normal] ?? ICON_REGISTRY[key] ?? null) as unknown;
  });
}
